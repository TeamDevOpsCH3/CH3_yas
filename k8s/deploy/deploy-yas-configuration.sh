#!/usr/bin/env bash
# =============================================================================
# deploy-yas-configuration.sh — Cài yas-configuration (ConfigMap+Secret dùng
# chung) vào namespace theo env. Methyl CH3 — YAS CD (C12+C18).
# Chạy TRƯỚC deploy-yas-applications.sh (app cần config sẵn).
# -----------------------------------------------------------------------------
# Cách dùng (từ thư mục k8s/deploy/):
#   ./deploy-yas-configuration.sh <env>     # env = baseline | dev | staging
#
# Mỗi ns app cần 1 bản yas-configuration; endpoint hạ tầng trong đó trỏ
# cross-namespace (postgresql.postgres, kafka-cluster-kafka-brokers.kafka, ...).
# =============================================================================
set -euo pipefail

ENV="${1:?Thiếu env. Dùng: baseline | dev | staging}"
CHARTS="${CHARTS:-../charts}"

case "$ENV" in
  baseline) NS="yas";;
  dev)      NS="dev";;
  staging)  NS="staging";;
  *) echo "❌ env phải là baseline | dev | staging"; exit 1;;
esac

DRY="${DRY_RUN:-0}"
echo "▶  yas-configuration -> ns/$NS  (ENV=$ENV, DRY_RUN=$DRY)"

# Reloader (stakater) để pod tự restart khi ConfigMap/Secret đổi
helm repo add stakater https://stakater.github.io/stakater-charts >/dev/null 2>&1 || true
helm repo update >/dev/null 2>&1 || true
helm dependency build "$CHARTS/yas-configuration" >/dev/null 2>&1 || true

if [ "$DRY" = "1" ]; then
  echo "── render: yas-configuration ───────────────────────────"
  helm template yas-configuration "$CHARTS/yas-configuration" -n "$NS"
else
  helm upgrade --install yas-configuration "$CHARTS/yas-configuration" \
    --namespace "$NS" --create-namespace
  echo "✅  yas-configuration đã cài vào ns/$NS. Giờ chạy: ./deploy-yas-applications.sh $ENV"
fi
