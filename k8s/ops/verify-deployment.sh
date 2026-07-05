#!/usr/bin/env bash
# =============================================================================
# verify-deployment.sh — Smoke test TỔNG THỂ YAS deployment (ns dev)
# Chạy TRÊN: worker-hiep WSL2 (có kubectl + reach master qua Tailscale)
# Mục đích: 1 lượt kiểm tra toàn bộ trang + service trước demo/nộp.
#
# Dùng --resolve trỏ thẳng master IP (không phụ thuộc DNS máy client).
# =============================================================================
set -uo pipefail

MASTER_IP="${MASTER_IP:-100.98.171.67}"
NS="${NS:-dev}"
PASS=0; FAIL=0; WARN=0

# --- màu ---
G='\033[0;32m'; R='\033[0;31m'; Y='\033[0;33m'; B='\033[0;34m'; N='\033[0m'

ok()   { echo -e "  ${G}✓ PASS${N} $1"; PASS=$((PASS+1)); }
bad()  { echo -e "  ${R}✗ FAIL${N} $1"; FAIL=$((FAIL+1)); }
warn() { echo -e "  ${Y}! WARN${N} $1"; WARN=$((WARN+1)); }
sec()  { echo -e "\n${B}=== $1 ===${N}"; }

# HTTP check qua ingress (--resolve): $1=host $2=path $3=expected_codes(regex) $4=label
http() {
  local host="$1" path="$2" want="$3" label="$4"
  local code
  code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 15 \
    --resolve "${host}:80:${MASTER_IP}" "http://${host}${path}" 2>/dev/null || echo "000")
  if [[ "$code" =~ $want ]]; then ok "$label ($code)"; else bad "$label (got $code, want $want)"; fi
}

echo "================================================================"
echo " YAS Deployment Smoke Test — ns=$NS master=$MASTER_IP"
echo " $(date)"
echo "================================================================"

# ─────────────────────────────────────────────────────────────────
sec "A. CLUSTER HEALTH"
NODES_READY=$(kubectl get nodes --no-headers 2>/dev/null | grep -c ' Ready ')
NODES_TOTAL=$(kubectl get nodes --no-headers 2>/dev/null | wc -l)
[ "$NODES_READY" = "$NODES_TOTAL" ] && ok "Nodes $NODES_READY/$NODES_TOTAL Ready" \
  || warn "Nodes $NODES_READY/$NODES_TOTAL Ready (worker-hoa hay down?)"

NOTREADY_PODS=$(kubectl -n $NS get pods --no-headers 2>/dev/null | grep -vE 'Running|Completed' | wc -l)
[ "$NOTREADY_PODS" = "0" ] && ok "Tất cả pod dev Running" \
  || { bad "$NOTREADY_PODS pod dev bất thường:"; kubectl -n $NS get pods --no-headers | grep -vE 'Running|Completed' | sed 's/^/      /'; }

# ─────────────────────────────────────────────────────────────────
sec "B. UI PAGES (qua ingress)"
http "storefront.dev.yas.local.com" "/"                       "200"     "Storefront trang chủ"
http "storefront.dev.yas.local.com" "/products/iphone-15"     "200"     "Storefront product detail (SSR)"
http "backoffice.dev.yas.local.com" "/"                       "200|302" "Backoffice (302=login OK)"
http "api.dev.yas.local.com"        "/swagger-ui"             "200|301|302" "Swagger UI"

# ─────────────────────────────────────────────────────────────────
sec "C. STOREFRONT API"
SF="storefront.dev.yas.local.com"
http "$SF" "/api/product/storefront/products/featured?pageNo=0" "200" "featured products"
http "$SF" "/api/product/storefront/categories"                 "200" "categories"
http "$SF" "/api/product/storefront/product/iphone-15"         "200|404" "product by slug (list-style)"
# profile/items 401/403 khi chưa login = ĐÚNG (không tính fail)
http "$SF" "/api/cart/storefront/cart/items"                    "401|403|200" "cart items (401/403=chưa login OK)"

# ─────────────────────────────────────────────────────────────────
sec "D. MEDIA IMAGES"
http "api.dev.yas.local.com" "/media/medias/7/file/iphone15_thumbnail.jpg"       "200" "product thumbnail"
http "api.dev.yas.local.com" "/media/medias/1/file/phone_category.jpg"           "200" "category image"

# ─────────────────────────────────────────────────────────────────
sec "E. AUTH / KEYCLOAK"
http "identity.yas.local.com" "/realms/Yas/.well-known/openid-configuration" "200" "Keycloak realm Yas OIDC"

# ─────────────────────────────────────────────────────────────────
sec "F. INFRA SERVICES (pod Running)"
for infra in postgres kafka elasticsearch redis keycloak; do
  cnt=$(kubectl -n "$infra" get pods --no-headers 2>/dev/null | grep -c 'Running')
  tot=$(kubectl -n "$infra" get pods --no-headers 2>/dev/null | wc -l)
  if [ "$tot" -gt 0 ] && [ "$cnt" = "$tot" ]; then ok "ns $infra: $cnt/$tot Running"
  elif [ "$tot" = "0" ]; then warn "ns $infra: không có pod (ns khác tên?)"
  else warn "ns $infra: $cnt/$tot Running"; fi
done

# ─────────────────────────────────────────────────────────────────
sec "G. DNS RESOLUTION"
# G1. SSR: CoreDNS rewrite .dev (pod resolve host ingress → ClusterIP)
SSR_IP=$(kubectl -n $NS run dnschk-$$-a --image=busybox:1.36 --rm -i --restart=Never --quiet -- \
  nslookup storefront.dev.yas.local.com 2>/dev/null | awk '/^Address: /{print $2}' | tail -1)
[ -n "$SSR_IP" ] && ok "CoreDNS SSR: storefront.dev → $SSR_IP (in-cluster)" \
  || bad "CoreDNS SSR: storefront.dev.yas.local.com KHÔNG resolve (detail sẽ 500)"
# G2. CoreDNS cụm không vỡ
SVC_IP=$(kubectl -n $NS run dnschk-$$-b --image=busybox:1.36 --rm -i --restart=Never --quiet -- \
  nslookup product.dev.svc.cluster.local 2>/dev/null | awk '/^Address: /{print $2}' | tail -1)
[ -n "$SVC_IP" ] && ok "CoreDNS cluster DNS OK (product.dev.svc → $SVC_IP)" \
  || bad "CoreDNS cluster DNS VỠ (product.dev.svc không resolve)"

# ─────────────────────────────────────────────────────────────────
sec "H. SERVICE MESH / mTLS"
STRICT=$(kubectl -n $NS get peerauthentication -o jsonpath='{range .items[*]}{.metadata.name}={.spec.mtls.mode} {end}' 2>/dev/null)
echo "      PeerAuth: $STRICT"
echo "$STRICT" | grep -q 'STRICT' && ok "mTLS STRICT present" || warn "không thấy STRICT (kiểm mesh)"
SIDECARS=$(kubectl -n $NS get pods --no-headers 2>/dev/null | grep -c '2/2')
ok "Pod có sidecar (2/2): $SIDECARS"

# ─────────────────────────────────────────────────────────────────
sec "I. OBSERVABILITY (C30 — có thể scale 0 để tiết kiệm RAM)"
OBS=$(kubectl -n observability get pods --no-headers 2>/dev/null | grep -c 'Running')
if [ "$OBS" -gt 0 ]; then ok "observability: $OBS pod Running"
else warn "observability: 0 pod (đã scale down? bật lại khi demo C30)"; fi

# ─────────────────────────────────────────────────────────────────
echo ""
echo "================================================================"
echo -e " KẾT QUẢ: ${G}PASS=$PASS${N}  ${R}FAIL=$FAIL${N}  ${Y}WARN=$WARN${N}"
echo "================================================================"
[ "$FAIL" = "0" ] && echo -e "${G}→ Deployment OK cho demo/nộp.${N}" \
  || echo -e "${R}→ Có $FAIL mục FAIL — kiểm trước khi demo.${N}"
