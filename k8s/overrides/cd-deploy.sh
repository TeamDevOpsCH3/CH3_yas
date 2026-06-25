#!/usr/bin/env bash
# =============================================================================
# cd-deploy.sh — C12 + C18 : Override Helm values (image -> Docker Hub, tag,
#                 namespace, DB theo env) + deploy bộ app YAS "GIỮ theo thầy".
# Methyl CH3 — YAS CD.  KHÔNG sửa chart gốc: mọi customization qua -f + --set.
# -----------------------------------------------------------------------------
# Cách dùng (chạy từ thư mục k8s/overrides/, cạnh thư mục ../charts):
#   ./cd-deploy.sh baseline     # ns yas,     tag main,    DB gốc  (C12/C13)
#   ./cd-deploy.sh dev          # ns dev,     tag develop, DB *_dev (C18)
#   ./cd-deploy.sh staging      # ns staging, tag main,    DB *_staging (C18)
#
# Render thử KHÔNG apply (như checklist C12 — bắt lỗi trước):
#   DRY_RUN=1 ./cd-deploy.sh dev | grep -E 'image:|SPRING_DATASOURCE_URL'
#
# Biến môi trường ghi đè được: ORG (mặc định methylch3), CHARTS (../charts),
#   DOMAIN (host ingress theo env).
# =============================================================================
set -euo pipefail

ENV="${1:?Thiếu env. Dùng: baseline | dev | staging}"
ORG="${ORG:-methylch3}"
CHARTS="${CHARTS:-../charts}"
VALUES="values-${ENV}.yaml"

# --- Demo hybrid: vài service GHÌ CỨNG xuống node on-prem (máy Hiệp) ----------
# Phần còn lại đã ưu tiên droplet (affinity mềm trong values-*.yaml).
# ONPREM = service nhẹ ưu tiên (mềm) pool on-prem node-type=onprem (xem values-onprem.yaml).
ONPREM="${ONPREM:-storefront-ui backoffice-ui tax customer swagger-ui}"

case "$ENV" in
  baseline) NS="yas";     SUFFIX="";         DOMAIN="${DOMAIN:-yas.local.com}";;
  dev)      NS="dev";     SUFFIX="_dev";     DOMAIN="${DOMAIN:-dev.yas.local.com}";;
  staging)  NS="staging"; SUFFIX="_staging"; DOMAIN="${DOMAIN:-staging.yas.local.com}";;
  *) echo "❌ env phải là baseline | dev | staging"; exit 1;;
esac
[ -f "$VALUES" ] || { echo "❌ Không thấy $VALUES (chạy từ thư mục k8s/overrides/)"; exit 1; }

DRY="${DRY_RUN:-0}"
echo "▶  ENV=$ENV  NS=$NS  TAG(theo $VALUES)  ORG=$ORG  SUFFIX='${SUFFIX:-<none>}'  DRY_RUN=$DRY"

# ---- Bảng service GIỮ ---------------------------------------------------------
# name | family(backend|ui|swagger) | image-suffix Docker Hub | postgres-db (- = không dùng DB)
read -r -d '' SERVICES <<'EOF' || true
product         backend  yas-product         product
cart            backend  yas-cart            cart
order           backend  yas-order           order
customer        backend  yas-customer        customer
inventory       backend  yas-inventory       inventory
tax             backend  yas-tax             tax
media           backend  yas-media           media
search          backend  yas-search          search
sampledata      backend  yas-sampledata      sampledata
storefront-bff  backend  yas-storefront-bff  -
backoffice-bff  backend  yas-backoffice-bff  -
storefront-ui   ui       yas-storefront      -
backoffice-ui   ui       yas-backoffice      -
swagger-ui      swagger  -                   -
EOF

# ---- Prereq: cấu hình dùng chung (ConfigMap + Secret) vào ns này -------------
# Mỗi ns app cần 1 bản yas-configuration; endpoint hạ tầng trong đó trỏ
# cross-namespace (postgresql.postgres, kafka-cluster-kafka-brokers.kafka, ...).
if [ "$DRY" != "1" ]; then
  echo "▶  Cài yas-configuration vào ns/$NS"
  helm dependency build "$CHARTS/yas-configuration" >/dev/null 2>&1 || true
  helm upgrade --install yas-configuration "$CHARTS/yas-configuration" \
    --namespace "$NS" --create-namespace
fi

# ---- Loop deploy / render từng service ---------------------------------------
while read -r NAME FAMILY IMG DB; do
  [ -z "${NAME:-}" ] && continue
  CHART="$CHARTS/$NAME"
  SET=()

  # image override (swagger-ui dùng image official swaggerapi/swagger-ui -> bỏ qua)
  if [ "$FAMILY" != "swagger" ]; then
    SET+=(--set "${FAMILY}.image.repository=${ORG}/${IMG}")
  fi

  # DB theo env: chỉ service có DB + chỉ dev/staging mới gắn hậu tố
  if [ "$DB" != "-" ] && [ -n "$SUFFIX" ]; then
    SET+=(--set "${FAMILY}.databaseName=${DB}${SUFFIX}")
  fi

  # ingress host (nếu chart bật ingress): bff + swagger
  case "$NAME" in
    storefront-bff) SET+=(--set "backend.ingress.host=storefront.${DOMAIN}");;
    backoffice-bff) SET+=(--set "backend.ingress.host=backoffice.${DOMAIN}");;
    swagger-ui)     SET+=(--set "ingress.host=api.${DOMAIN}");;
  esac

  # Demo hybrid: service trong ONPREM ƯU TIÊN MỀM xuống node máy Hiệp qua overlay
  # values-onprem.yaml. Máy bật → chạy on-prem; máy tắt → tự nhảy lên droplet
  # (preferred = mềm, không kẹt Pending). Các service khác giữ "ưu tiên cloud".
  ONPREM_F=()
  if [[ " $ONPREM " == *" $NAME "* ]]; then
    ONPREM_F+=(-f "values-onprem.yaml")
    echo "   ⤷ $NAME ưu tiên (mềm) pool on-prem; cả 2 máy tắt thì rớt về droplet"
  fi

  helm dependency build "$CHART" >/dev/null 2>&1 || true

  if [ "$DRY" = "1" ]; then
    echo "── render: $NAME ($FAMILY) ─────────────────────────────"
    helm template "$NAME" "$CHART" -n "$NS" -f "$VALUES" "${ONPREM_F[@]}" "${SET[@]}"
  else
    echo "▶  deploy: $NAME -> ns/$NS  (${ORG}/${IMG:-official})"
    helm upgrade --install "$NAME" "$CHART" \
      --namespace "$NS" --create-namespace \
      -f "$VALUES" "${ONPREM_F[@]}" "${SET[@]}"
    sleep 5
  fi
done <<< "$SERVICES"

echo "✅  Xong ENV=$ENV. Kiểm: kubectl -n $NS get pods -o wide"
