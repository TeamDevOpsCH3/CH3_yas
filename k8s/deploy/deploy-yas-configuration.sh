#!/usr/bin/env bash
# deploy-yas-configuration.sh — Deploy yas-configuration chart (ConfigMap + Secret)
# into the target namespace. Must run BEFORE deploy-yas-applications.sh.
#
# Usage (from k8s/deploy/):
#   ./deploy-yas-configuration.sh <env>   env = baseline | dev | staging
#
# Dry-run (render only, no apply):
#   DRY_RUN=1 ./deploy-yas-configuration.sh baseline
set -euo pipefail

ENV="${1:?Missing env. Usage: baseline | dev | staging}"
CHARTS="${CHARTS:-../charts}"

case "$ENV" in
  baseline) NS="yas";     DOMAIN="yas.local.com";;
  dev)      NS="dev";     DOMAIN="dev.yas.local.com";;
  staging)  NS="staging"; DOMAIN="staging.yas.local.com";;
  *) echo "ERROR: env must be baseline | dev | staging"; exit 1;;
esac

DRY="${DRY_RUN:-0}"
# media publicUrl must carry the per-env domain (browser fetches images from here)
MEDIA_PUBLIC_URL="http://api.${DOMAIN}/media"
echo "ENV=$ENV  NS=$NS  DOMAIN=$DOMAIN  DRY_RUN=$DRY"

# Reloader (Stakater): auto-restarts pods when ConfigMap/Secret changes
helm repo add stakater https://stakater.github.io/stakater-charts >/dev/null 2>&1 || true
helm repo update >/dev/null 2>&1 || true
helm dependency build "$CHARTS/yas-configuration" >/dev/null 2>&1 || true

if [ "$DRY" = "1" ]; then
  helm template yas-configuration "$CHARTS/yas-configuration" -n "$NS" \
    --set mediaApplicationConfig.yas.publicUrl="$MEDIA_PUBLIC_URL"
else
  helm upgrade --install yas-configuration "$CHARTS/yas-configuration" \
    --namespace "$NS" --create-namespace \
    --set mediaApplicationConfig.yas.publicUrl="$MEDIA_PUBLIC_URL"
  echo "yas-configuration deployed to ns/$NS (media publicUrl=$MEDIA_PUBLIC_URL)."
  echo "Next: ./deploy-yas-applications.sh $ENV"
fi
