#!/usr/bin/env bash
# =============================================================================
# deploy-yas-applications.sh — Deploy bộ app YAS (GIỮ theo keep-list) theo env.
# Methyl CH3 — YAS CD (C12 + C18). Gộp từ cd-deploy.sh, đặt trong k8s/deploy/
# theo chuẩn README thầy (README trỏ đích danh deploy-yas-applications.sh).
# KHÔNG sửa chart gốc: mọi customization qua -f + --set.
# -----------------------------------------------------------------------------
# Cách dùng (chạy từ thư mục k8s/deploy/, cạnh ../charts):
#   ./deploy-yas-configuration.sh <env>   # chạy TRƯỚC (cài config vào ns)
#   ./deploy-yas-applications.sh  <env>   # rồi chạy cái này
#
#   env = baseline | dev | staging
#     baseline -> ns yas,     DB gốc      (C12/C13)
#     dev      -> ns dev,      DB *_dev    (C18, tag develop)
#     staging  -> ns staging,  DB *_staging(C18, tag main)
#
# Render thử KHÔNG apply (bắt lỗi trước):
#   DRY_RUN=1 ./deploy-yas-applications.sh dev | grep -E 'image:|SPRING_DATASOURCE'
#
# Ghi đè được qua env: ORG (mặc định methylch3), CHARTS (../charts), DOMAIN.
# =============================================================================
set -euo pipefail

ENV="${1:?Thiếu env. Dùng: baseline | dev | staging}"
ORG="${ORG:-methylch3}"
CHARTS="${CHARTS:-../charts}"
VALUES="values-${ENV}.yaml"

# Service ưu tiên (mềm) xuống node on-prem (máy Hiệp) qua values-onprem.yaml.
ONPREM="${ONPREM:-storefront-ui backoffice-ui tax customer swagger-ui}"

# Hikari pool mỗi service Postgres (chart mặc định 10). Giảm 5 để 3 môi trường
# (yas+dev+staging) không vượt max_connections=200 của Postgres.
HIKARI_POOL="${HIKARI_POOL:-5}"

case "$ENV" in
  baseline) NS="yas";     SUFFIX="";         DOMAIN="${DOMAIN:-yas.local.com}";;
  dev)      NS="dev";     SUFFIX="_dev";     DOMAIN="${DOMAIN:-dev.yas.local.com}";;
  staging)  NS="staging"; SUFFIX="_staging"; DOMAIN="${DOMAIN:-staging.yas.local.com}";;
  *) echo "❌ env phải là baseline | dev | staging"; exit 1;;
esac
[ -f "$VALUES" ] || { echo "❌ Không thấy $VALUES (chạy từ thư mục k8s/deploy/)"; exit 1; }

DRY="${DRY_RUN:-0}"
echo "▶  ENV=$ENV  NS=$NS  ORG=$ORG  SUFFIX='${SUFFIX:-<none>}'  DRY_RUN=$DRY"
echo "ℹ  Nhớ chạy ./deploy-yas-configuration.sh $ENV TRƯỚC file này."

# ---- Bảng service GIỮ (keep-list) --------------------------------------------
# name | family(backend|ui|swagger) | image-suffix Docker Hub | postgres-db (- = không DB)
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

# ---- Loop deploy / render từng service ---------------------------------------
while read -r NAME FAMILY IMG DB; do
  [ -z "${NAME:-}" ] && continue
  CHART="$CHARTS/$NAME"
  SET=()

  # image override (swagger-ui dùng image official -> bỏ qua)
  if [ "$FAMILY" != "swagger" ]; then
    SET+=(--set "${FAMILY}.image.repository=${ORG}/${IMG}")
  fi

  # DB theo env: chỉ service có DB + chỉ dev/staging mới gắn hậu tố
  if [ "$DB" != "-" ] && [ -n "$SUFFIX" ]; then
    SET+=(--set "${FAMILY}.databaseName=${DB}${SUFFIX}")
  fi

  # Hikari pool cho service dùng Postgres
  if [ "$DB" != "-" ]; then
    SET+=(--set "${FAMILY}.extraEnvs[0].name=SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE")
    SET+=(--set-string "${FAMILY}.extraEnvs[0].value=${HIKARI_POOL}")
  fi

  # ingress host (bff + swagger)
  case "$NAME" in
    storefront-bff) SET+=(--set "backend.ingress.host=storefront.${DOMAIN}");;
    backoffice-bff) SET+=(--set "backend.ingress.host=backoffice.${DOMAIN}");;
    swagger-ui)     SET+=(--set "ingress.host=api.${DOMAIN}");;
  esac

  # Demo hybrid: ONPREM ưu tiên mềm xuống máy Hiệp; máy tắt thì rớt về droplet
  ONPREM_F=()
  if [[ " $ONPREM " == *" $NAME "* ]]; then
    ONPREM_F+=(-f "values-onprem.yaml")
    echo "   ⤷ $NAME ưu tiên (mềm) pool on-prem"
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
