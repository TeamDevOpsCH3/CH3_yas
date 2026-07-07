#!/bin/bash
# setup-keycloak.sh — Deploy Keycloak (Bitnami) into namespace keycloak.
# Uses shared PostgreSQL from namespace postgres (must be Running first).
# Replaces: Keycloak Operator CRD (see keycloak/keycloak.operator.bak/).
#
# Call order: ./setup-cluster.sh [datastore] → ./setup-keycloak.sh
set -euo pipefail

read -rd '' DOMAIN POSTGRESQL_USERNAME POSTGRESQL_PASSWORD \
  BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD \
  KEYCLOAK_BACKOFFICE_REDIRECT_URL KEYCLOAK_STOREFRONT_REDIRECT_URL \
  < <(yq -r '.domain,
    .postgresql.username, .postgresql.password,
    .keycloak.bootstrapAdmin.username, .keycloak.bootstrapAdmin.password,
    .keycloak.backofficeRedirectUrl, .keycloak.storefrontRedirectUrl' \
    ./cluster-config.yaml)

echo "[keycloak] waiting for PostgreSQL..."
kubectl rollout status statefulset/postgresql -n postgres --timeout=180s

helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
helm repo update bitnami

echo "[keycloak] deploying..."
helm upgrade --install keycloak bitnami/keycloak \
  --create-namespace --namespace keycloak \
  -f ./keycloak/values-bitnami.yaml \
  --set auth.adminUser="${BOOTSTRAP_ADMIN_USERNAME}" \
  --set auth.adminPassword="${BOOTSTRAP_ADMIN_PASSWORD}" \
  --set externalDatabase.user="${POSTGRESQL_USERNAME}" \
  --set externalDatabase.password="${POSTGRESQL_PASSWORD}"

echo "[keycloak] done. Verify: kubectl rollout status deploy/identity -n keycloak --timeout=300s"
echo "NodePort: kubectl get svc identity -n keycloak"
echo "Note: realm import (redirect URLs) must be applied separately via KeycloakRealmImport or admin UI."
echo "  Backoffice: ${KEYCLOAK_BACKOFFICE_REDIRECT_URL}"
echo "  Storefront: ${KEYCLOAK_STOREFRONT_REDIRECT_URL}"
