#!/bin/bash
# setup-cluster.sh — Deploy shared infrastructure for YAS cluster.
# Data store uses Bitnami (replaces Zalando/Strimzi/ECK operators; see *.operator.bak/).
# Observability stack unchanged from upstream.
#
# Usage:
#   ./setup-cluster.sh [all|datastore|observability]  (default: all)
#
# Call order:
#   ./setup-cluster.sh → ./setup-keycloak.sh → ./setup-redis.sh
#   → ./deploy-yas-configuration.sh <env> → ./deploy-yas-applications.sh <env>
set -euo pipefail

MODE="${1:-all}"

read -rd '' DOMAIN POSTGRESQL_USERNAME POSTGRESQL_PASSWORD \
  GRAFANA_USERNAME GRAFANA_PASSWORD \
  < <(yq -r '.domain,
    .postgresql.username, .postgresql.password,
    .grafana.username, .grafana.password' ./cluster-config.yaml) || true

setup_datastore() {
  helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
  helm repo add akhq    https://akhq.io/                  2>/dev/null || true
  helm repo update

  echo "[postgres] deploying..."
  helm upgrade --install postgresql bitnami/postgresql \
    --create-namespace --namespace postgres \
    -f ./postgres/values-bitnami.yaml
  bash ./postgres/create-databases.sh

  echo "[elasticsearch] deploying..."
  helm upgrade --install elasticsearch bitnami/elasticsearch \
    --create-namespace --namespace elasticsearch \
    -f ./elasticsearch/values-bitnami.yaml

  echo "[kafka] deploying..."
  helm upgrade --install kafka bitnami/kafka \
    --create-namespace --namespace kafka \
    -f ./kafka/values-bitnami.yaml

  echo "[akhq] deploying..."
  helm upgrade --install akhq akhq/akhq \
    --create-namespace --namespace kafka \
    --values ./kafka/akhq.values.yaml

  echo "[datastore] done. Verify: kubectl get pods -n postgres -n kafka -n elasticsearch"
}

setup_observability() {
  helm repo add grafana               https://grafana.github.io/helm-charts                      2>/dev/null || true
  helm repo add prometheus-community  https://prometheus-community.github.io/helm-charts          2>/dev/null || true
  helm repo add open-telemetry        https://open-telemetry.github.io/opentelemetry-helm-charts  2>/dev/null || true
  helm repo add jetstack              https://charts.jetstack.io                                  2>/dev/null || true
  helm repo update

  echo "[loki] deploying..."
  helm upgrade --install loki grafana/loki \
    --create-namespace --namespace observability \
    -f ./observability/loki.values.yaml

  echo "[tempo] deploying..."
  helm upgrade --install tempo grafana/tempo \
    --create-namespace --namespace observability \
    -f ./observability/tempo.values.yaml

  echo "[cert-manager] deploying..."
  helm upgrade --install cert-manager jetstack/cert-manager \
    --namespace cert-manager --create-namespace \
    --version v1.12.0 \
    --set installCRDs=true \
    --set prometheus.enabled=false \
    --set webhook.timeoutSeconds=4 \
    --set admissionWebhooks.certManager.create=true

  echo "[opentelemetry-operator] deploying..."
  helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
    --create-namespace --namespace observability

  echo "[opentelemetry-collector] deploying..."
  helm upgrade --install opentelemetry-collector ./observability/opentelemetry \
    --create-namespace --namespace observability

  echo "[promtail] deploying..."
  helm upgrade --install promtail grafana/promtail \
    --create-namespace --namespace observability \
    --values ./observability/promtail.values.yaml

  echo "[prometheus/grafana] deploying..."
  grafana_hostname="grafana.${DOMAIN}" yq -i '.hostname=env(grafana_hostname)' \
    ./observability/prometheus.values.yaml
  postgresql_username="${POSTGRESQL_USERNAME}" yq -i \
    '.grafana."grafana.ini".database.user=env(postgresql_username)' \
    ./observability/prometheus.values.yaml
  postgresql_password="${POSTGRESQL_PASSWORD}" yq -i \
    '.grafana."grafana.ini".database.password=env(postgresql_password)' \
    ./observability/prometheus.values.yaml
  helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
    --create-namespace --namespace observability \
    -f ./observability/prometheus.values.yaml

  echo "[grafana-operator] deploying..."
  helm upgrade --install grafana-operator \
    oci://ghcr.io/grafana-operator/helm-charts/grafana-operator \
    --version v5.0.2 \
    --create-namespace --namespace observability

  echo "[grafana-config] deploying..."
  helm upgrade --install grafana ./observability/grafana \
    --create-namespace --namespace observability \
    --set hostname="grafana.${DOMAIN}" \
    --set grafana.username="${GRAFANA_USERNAME}" \
    --set grafana.password="${GRAFANA_PASSWORD}" \
    --set postgresql.username="${POSTGRESQL_USERNAME}" \
    --set postgresql.password="${POSTGRESQL_PASSWORD}"

  echo "[observability] done. Verify: kubectl get pods -n observability"
}

case "$MODE" in
  datastore)     setup_datastore ;;
  observability) setup_observability ;;
  all)           setup_datastore; setup_observability ;;
  *)             echo "Usage: $0 [all|datastore|observability]"; exit 1 ;;
esac

echo "setup-cluster.sh complete (mode=$MODE)."