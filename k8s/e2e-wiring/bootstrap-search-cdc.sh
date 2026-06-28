#!/usr/bin/env bash
# ============================================================================
# bootstrap-search-cdc.sh — Dựng lại toàn bộ pipeline SEARCH + CDC sau rebuild.
# Chạy trên: worker-hiep WSL2 (sau khi infra + app dev đã lên).
#
# Lo 6 tầng (idempotent — chạy lại an toàn):
#   [1] ES node role data (red->yellow)
#   [2] nginx FQDN (đã trong 02-configmap, chỉ verify)
#   [3] Postgres wal_level=logical (+ restart)
#   [4] Publication dbz_product
#   [5] Kafka RAM limit nâng (tránh OOM khi chạy Connect)
#   [6] Debezium Connect + connector -> sync product->ES
#
# VÌ SAO TÁCH KHỎI bootstrap-c15.sh: CDC là phần nặng (restart PG/Kafka,
# +RAM), optional cho demo cơ bản. Search chỉ cần khi demo AuthorizationPolicy.
#
# LƯU Ý: 3 infra patch (ES role, PG wal, Kafka RAM) áp lên Bitnami runtime
# vì file repo k8s/deploy/{elasticsearch,postgres,kafka} là operator cũ
# (ECK/Zalando/...) KHÔNG dùng. Patch runtime là cách đúng cho Bitnami hiện tại.
# ============================================================================
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
c_g="\033[0;32m"; c_y="\033[0;33m"; c_r="\033[0;31m"; c_x="\033[0m"
log(){ echo -e "${c_g}==>${c_x} $*"; }
warn(){ echo -e "${c_y}[!]${c_x} $*"; }
err(){ echo -e "${c_r}[x]${c_x} $*" >&2; }

# --- [1] ES node role data ---
log "[1/6] ES node role data (fix index red)"
roles=$(kubectl -n elasticsearch get statefulset elasticsearch-master \
  -o jsonpath='{range .spec.template.spec.containers[0].env[*]}{.name}={.value}{"\n"}{end}' 2>/dev/null \
  | grep ELASTICSEARCH_NODE_ROLES | cut -d= -f2)
if echo "$roles" | grep -q 'data'; then
  warn "ES role đã có data ($roles) — bỏ qua."
else
  kubectl -n elasticsearch set env statefulset/elasticsearch-master \
    ELASTICSEARCH_NODE_ROLES=master,data,ingest
  log "Đợi ES restart..."
  kubectl -n elasticsearch rollout status statefulset/elasticsearch-master --timeout=180s
fi

# --- [2] nginx FQDN (verify - đã trong 02-configmap) ---
log "[2/6] nginx FQDN route (verify)"
if kubectl -n dev get configmap nginx-gateway-conf -o jsonpath='{.data.default\.conf\.template}' 2>/dev/null \
   | grep -q 'search.dev.svc.cluster.local'; then
  warn "nginx search FQDN đã có — OK."
else
  warn "nginx chưa FQDN — apply lại 02-configmap"
  kubectl apply -f "$SCRIPT_DIR/02-nginx-gateway-configmap.yaml"
  kubectl -n dev rollout restart deploy/nginx
fi

# --- [3] Postgres wal_level=logical ---
log "[3/6] Postgres wal_level=logical"
export PGPW=$(kubectl -n postgres get secret postgresql -o jsonpath='{.data.postgres-password}' | base64 -d)
wal=$(kubectl -n postgres exec statefulset/postgresql -- env PGPASSWORD="$PGPW" \
  psql -U postgres -tAc "SHOW wal_level;" 2>/dev/null | tr -d '[:space:]')
if [ "$wal" = "logical" ]; then
  warn "wal_level đã logical — bỏ qua."
else
  warn "wal_level=$wal -> đổi logical (RESTART Postgres - cả cụm gián đoạn ~30s)"
  kubectl -n postgres set env statefulset/postgresql POSTGRESQL_WAL_LEVEL=logical
  kubectl -n postgres rollout status statefulset/postgresql --timeout=180s
  sleep 5
fi

# --- [4] Publication ---
log "[4/6] Publication dbz_product"
pub=$(kubectl -n postgres exec statefulset/postgresql -- env PGPASSWORD="$PGPW" \
  psql -U postgres -d product_dev -tAc "SELECT 1 FROM pg_publication WHERE pubname='dbz_product';" 2>/dev/null | tr -d '[:space:]')
if [ "$pub" = "1" ]; then
  warn "Publication dbz_product đã có — bỏ qua."
else
  kubectl -n postgres exec statefulset/postgresql -- env PGPASSWORD="$PGPW" \
    psql -U postgres -d product_dev -c "CREATE PUBLICATION dbz_product FOR ALL TABLES;"
fi

# --- [5] Kafka RAM limit ---
log "[5/6] Kafka memory limit (tránh OOM khi chạy Connect)"
lim=$(kubectl -n kafka get statefulset kafka-cluster-kafka-brokers-controller \
  -o jsonpath='{.spec.template.spec.containers[0].resources.limits.memory}' 2>/dev/null)
if [ "$lim" = "1280Mi" ]; then
  warn "Kafka limit đã 1280Mi — bỏ qua."
else
  warn "Kafka limit=$lim -> nâng 1280Mi (rolling restart brokers)"
  kubectl -n kafka patch statefulset kafka-cluster-kafka-brokers-controller --type='json' -p='[
    {"op":"replace","path":"/spec/template/spec/containers/0/resources/limits/memory","value":"1280Mi"},
    {"op":"replace","path":"/spec/template/spec/containers/0/resources/requests/memory","value":"768Mi"}]'
  kubectl -n kafka rollout status statefulset/kafka-cluster-kafka-brokers-controller --timeout=300s
fi

# --- [6] Debezium Connect + connector ---
log "[6/6] Debezium Connect + connector"
if ! kubectl -n kafka get deploy debezium-connect >/dev/null 2>&1; then
  kubectl apply -f "$SCRIPT_DIR/08-debezium-connect.yaml"
  kubectl -n kafka rollout status deploy/debezium-connect --timeout=240s
else
  warn "Connect deploy đã có."
fi
# đợi Connect REST sẵn sàng
for i in $(seq 1 20); do
  up=$(kubectl -n kafka run cdccheck-$$-$i --image=curlimages/curl --rm -i --restart=Never -- \
    curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://debezium-connect.kafka:8083/ 2>/dev/null | tr -dc '0-9')
  [ "$up" = "200" ] && break
  sleep 6
done
# tạo connector nếu chưa có
has=$(kubectl -n kafka run cdccheck2-$$ --image=curlimages/curl --rm -i --restart=Never -- \
  curl -s --max-time 5 http://debezium-connect.kafka:8083/connectors 2>/dev/null | grep -c product-connector || echo 0)
if [ "$has" = "0" ]; then
  log "Tạo connector product-connector (thay password runtime)"
  POD=$(kubectl -n kafka get pod -l app=debezium-connect -o jsonpath='{.items[0].metadata.name}')
  kubectl -n kafka cp "$SCRIPT_DIR/debezium-connector-product.json" "$POD:/tmp/connector.json"
  kubectl -n kafka exec "$POD" -- sh -c "
    sed -i 's/__PGPW__/$PGPW/' /tmp/connector.json
    curl -s -X POST http://localhost:8083/connectors -H 'Content-Type: application/json' -d @/tmp/connector.json
    rm -f /tmp/connector.json"
  echo ""
else
  warn "Connector product-connector đã có."
fi

# --- Verify chuỗi ---
log "Verify: ES docs count (kỳ vọng >0 sau snapshot)"
sleep 8
cnt=$(kubectl -n dev run cdcverify-$$ --image=curlimages/curl --rm -i --restart=Never -- \
  curl -s "http://elasticsearch-es-http.elasticsearch:9200/product/_count" 2>/dev/null | grep -oE '"count":[0-9]+' | cut -d: -f2)
echo ""
if [ -n "$cnt" ] && [ "$cnt" -gt 0 ] 2>/dev/null; then
  log "✓ SEARCH CDC OK — ES product = $cnt docs"
else
  warn "ES docs=$cnt — snapshot có thể đang chạy, đợi thêm 30s rồi check:"
  warn "  curl ES .../product/_count  |  kubectl -n kafka logs deploy/debezium-connect"
fi
echo ""
echo "Test: curl 'http://nginx/search/storefront/catalog-search?keyword=iphone'"
