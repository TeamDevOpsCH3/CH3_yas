#!/usr/bin/env bash
# deploy-yas-applications.sh — Deploy YAS application services per environment.
# Run AFTER deploy-yas-configuration.sh (apps depend on ConfigMap/Secret being present).
#
# Usage (from k8s/deploy/):
#   ./deploy-yas-configuration.sh <env>   # must run first
#   ./deploy-yas-applications.sh  <env>   # then this
#
#   env = baseline | dev | staging
#     baseline → ns yas,     image tag main,         DB: <db>
#     dev      → ns dev,     image tag <commit-sha>, DB: <db>_dev
#     staging  → ns staging, image tag main,         DB: <db>_staging
#
# Dry-run (render only, no apply):
#   DRY_RUN=1 ./deploy-yas-applications.sh baseline | grep -E 'image:|SPRING_DATASOURCE'
#
# Overrides:
#   ORG=<dockerhub-org>  CHARTS=<path-to-charts>  HIKARI_POOL=<n>
# =============================================================================
set -euo pipefail

ENV="${1:?Missing env. Usage: baseline | dev | staging}"
ORG="${ORG:-methylch3}"
CHARTS="${CHARTS:-../charts}"
VALUES="values-${ENV}.yaml"

# Services that prefer on-prem nodes (soft affinity via values-onprem.yaml)
ONPREM="${ONPREM:-storefront-ui backoffice-ui tax customer swagger-ui}"

# Reduce Hikari pool size so 3 envs (yas+dev+staging) stay within Postgres max_connections=200
HIKARI_POOL="${HIKARI_POOL:-5}"

case "$ENV" in
  baseline) NS="yas";     SUFFIX="";         DOMAIN="${DOMAIN:-yas.local.com}";;
  dev)      NS="dev";     SUFFIX="_dev";     DOMAIN="${DOMAIN:-dev.yas.local.com}";;
  staging)  NS="staging"; SUFFIX="_staging"; DOMAIN="${DOMAIN:-staging.yas.local.com}";;
  *) echo "ERROR: env must be baseline | dev | staging"; exit 1;;
esac
[ -f "$VALUES" ] || { echo "ERROR: $VALUES not found (run from k8s/deploy/)"; exit 1; }

DRY="${DRY_RUN:-0}"
echo "ENV=$ENV  NS=$NS  ORG=$ORG  SUFFIX='${SUFFIX:-<none>}'  DRY_RUN=$DRY"
echo "Note: run ./deploy-yas-configuration.sh $ENV first if not done."

# =============================================================================
# SERVICE TABLE — 13 persistent services (sampledata handled separately below)
# Columns: name | family(backend|ui|swagger) | docker-image-suffix | postgres-db (- = no DB)
# =============================================================================
read -r -d '' SERVICES <<'EOF' || true
product         backend  yas-product         product
cart            backend  yas-cart            cart
order           backend  yas-order           order
customer        backend  yas-customer        customer
inventory       backend  yas-inventory       inventory
tax             backend  yas-tax             tax
media           backend  yas-media           media
search          backend  yas-search          search
storefront-bff  backend  yas-storefront-bff  -
backoffice-bff  backend  yas-backoffice-bff  -
storefront-ui   ui       yas-storefront      -
backoffice-ui   ui       yas-backoffice      -
swagger-ui      swagger  -                   -
EOF

# =============================================================================
# DEPLOY LOOP — persistent services
# =============================================================================
while read -r NAME FAMILY IMG DB; do
  [ -z "${NAME:-}" ] && continue
  CHART="$CHARTS/$NAME"
  SET=()

  # Image override (swagger-ui uses official image, skip)
  if [ "$FAMILY" != "swagger" ]; then
    SET+=(--set "${FAMILY}.image.repository=${ORG}/${IMG}")
  fi

  # DB name suffix per env
  if [ "$DB" != "-" ] && [ -n "$SUFFIX" ]; then
    SET+=(--set "${FAMILY}.databaseName=${DB}${SUFFIX}")
  fi

  # Hikari pool size for Postgres-backed services
  if [ "$DB" != "-" ]; then
    SET+=(--set "${FAMILY}.extraEnvs[0].name=SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE")
    SET+=(--set-string "${FAMILY}.extraEnvs[0].value=${HIKARI_POOL}")
  fi

  # Ingress host for BFF and swagger
  case "$NAME" in
    storefront-bff) SET+=(--set "backend.ingress.host=storefront.${DOMAIN}");;
    backoffice-bff) SET+=(--set "backend.ingress.host=backoffice.${DOMAIN}");;
    swagger-ui)     SET+=(--set "ingress.host=api.${DOMAIN}");;
  esac

  # Soft affinity toward on-prem nodes for listed services
  ONPREM_F=()
  if [[ " $ONPREM " == *" $NAME "* ]]; then
    ONPREM_F+=(-f "values-onprem.yaml")
  fi

  helm dependency build "$CHART" >/dev/null 2>&1 || true

  if [ "$DRY" = "1" ]; then
    echo "── render: $NAME ($FAMILY) ──────────────────────────────"
    helm template "$NAME" "$CHART" -n "$NS" -f "$VALUES" "${ONPREM_F[@]}" "${SET[@]}"
  else
    echo "[deploy] $NAME → ns/$NS  (${ORG}/${IMG:-official})"
    helm upgrade --install "$NAME" "$CHART" \
      --namespace "$NS" --create-namespace \
      -f "$VALUES" "${ONPREM_F[@]}" "${SET[@]}"
    sleep 3
  fi
done <<< "$SERVICES"

# =============================================================================
# SAMPLEDATA — run-once seeding job
# Deploy, wait for seed to complete, then scale to 0 replicas.
# Re-run only if data needs to be reset.
# =============================================================================
if [ "$DRY" != "1" ]; then
  echo ""
  echo "[sampledata] deploying seed job (run-once)..."
  SAMPLEDATA_SET=(
    --set "backend.image.repository=${ORG}/yas-sampledata"
    --set "backend.extraEnvs[0].name=SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE"
    --set-string "backend.extraEnvs[0].value=${HIKARI_POOL}"
  )
  [ -n "$SUFFIX" ] && SAMPLEDATA_SET+=(--set "backend.databaseName=sampledata${SUFFIX}")

  helm upgrade --install sampledata "$CHARTS/sampledata" \
    --namespace "$NS" --create-namespace \
    -f "$VALUES" "${SAMPLEDATA_SET[@]}"

  echo "[sampledata] waiting for seed to complete (timeout 5m)..."
  if kubectl rollout status deployment/sampledata -n "$NS" --timeout=300s 2>/dev/null; then
    echo "[sampledata] seed complete. Scaling down to 0 replicas..."
    kubectl scale deployment/sampledata -n "$NS" --replicas=0
    echo "[sampledata] scaled down. Re-run with 'kubectl scale deploy/sampledata -n $NS --replicas=1' to reseed."
  else
    echo "[sampledata] WARNING: rollout timed out. Check: kubectl logs -n $NS deploy/sampledata"
  fi
fi

# =============================================================================
# VERIFY
# =============================================================================
if [ "$DRY" != "1" ]; then
  echo ""
  echo "=== DEPLOY COMPLETE (ENV=$ENV) ==="
  echo "Verify pods:     kubectl get pods -n $NS -o wide"
  echo "Verify releases: helm list -n $NS"
  echo "Verify images:   kubectl get deploy -n $NS -o jsonpath='{range .items[*]}{.metadata.name}: {.spec.template.spec.containers[0].image}{\"\\n\"}{end}'"
  echo "Verify endpoints:kubectl get endpoints -n $NS | grep -v '<none>'"
fi
