#!/usr/bin/env bash
# Create YAS databases in the shared PostgreSQL instance.
# Idempotent: safe to re-run (existing databases are skipped).
# Usage: ./postgres/create-databases.sh (run from k8s/deploy/)
set -euo pipefail

NS="postgres"
STS="postgresql"
PGUSER="postgres"
PGPW="admin"

# 9 services with dedicated databases × 3 environments + keycloak = 28 total
SERVICES="product cart order customer inventory tax media search sampledata"

echo "Waiting for PostgreSQL to be ready..."
kubectl rollout status statefulset/${STS} -n ${NS} --timeout=120s

for db in ${SERVICES}; do
  for suffix in "" "_dev" "_staging"; do
    kubectl exec -i statefulset/${STS} -n ${NS} -- \
      env PGPASSWORD=${PGPW} psql -U ${PGUSER} \
      -c "CREATE DATABASE \"${db}${suffix}\";" 2>&1 \
      | grep -v "already exists" || true
  done
done

kubectl exec -i statefulset/${STS} -n ${NS} -- \
  env PGPASSWORD=${PGPW} psql -U ${PGUSER} \
  -c 'CREATE DATABASE "keycloak";' 2>&1 \
  | grep -v "already exists" || true

echo "Done. Verify: kubectl exec -i statefulset/${STS} -n ${NS} -- env PGPASSWORD=${PGPW} psql -U ${PGUSER} -c '\l'"
