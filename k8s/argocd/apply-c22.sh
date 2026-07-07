#!/usr/bin/env bash
# =============================================================================
# apply-c22.sh — Bootstrap ArgoCD Applications for an env (C22, Automation/DR).
# Methyl CH3 — YAS CD.
#
# Two modes:
#   ./apply-c22.sh dev            # STAGED ADOPT: apply appset, then print the
#                                 # manual diff/sync checkpoints (safe on a live
#                                 # dev cluster — no auto prune/self-heal yet).
#   ./apply-c22.sh dev --auto     # FULL AUTO (disaster-recovery): apply, sync
#   ./apply-c22.sh staging --auto # all, wait healthy. Use on a fresh/empty ns.
#
# Fits the existing DR chain: rebuild-droplets -> setup-cluster ->
# deploy-yas-configuration -> apply-c22.sh <env> --auto  (GitOps takes over).
#
# PREREQ: `argocd` CLI logged in (sync/wait use it):
#   argocd login <node-ip>:32087 --insecure --username admin \
#     --password "$(kubectl -n argocd get secret argocd-initial-admin-secret \
#       -o jsonpath='{.data.password}' | base64 -d)"
#
# RUN ON: worker-hiep WSL2, from repo root.
# =============================================================================
set -euo pipefail

ENV="${1:?Usage: ./apply-c22.sh dev|staging [--auto]}"
AUTO=0; [ "${2:-}" = "--auto" ] && AUTO=1
DIR="$(cd "$(dirname "$0")" && pwd)"

case "$ENV" in
  dev)     APPSET="$DIR/appset-dev.yaml";;
  staging) APPSET="$DIR/appset-staging.yaml";;
  *) echo "ERROR: env must be dev|staging"; exit 1;;
esac
[ -f "$APPSET" ] || { echo "ERROR: not found $APPSET"; exit 1; }

echo "==> kubectl apply -f $(basename "$APPSET")"
kubectl apply -f "$APPSET"

echo "==> Generated Applications (env=$ENV):"
sleep 3
kubectl -n argocd get applications -l "env=$ENV" \
  -o custom-columns=NAME:.metadata.name,SYNC:.status.sync.status,HEALTH:.status.health.status

if [ "$AUTO" = "1" ]; then
  echo
  echo "==> --auto: syncing all apps (env=$ENV) and waiting for Healthy"
  argocd app sync -l "env=$ENV"
  argocd app wait -l "env=$ENV" --health --timeout 900
  echo "✅ All $ENV apps Synced + Healthy."
else
  cat <<EOF

==> STAGED ADOPT (safe on live cluster). Next, run yourself:

  # 1) Inspect ONE app render/diff — do NOT sync yet:
  argocd app get  yas-${ENV}-product
  argocd app diff yas-${ENV}-product      # expect only argocd/helm meta labels

  # 2) Diff clean -> sync product, verify, then sync the rest:
  argocd app sync yas-${ENV}-product
  argocd app sync -l env=${ENV}
  argocd app wait -l env=${ENV} --health --timeout 900

  # 3) Only after everything is Synced + Healthy -> enable auto-reconcile:
  #    uncomment 'automated: { prune, selfHeal }' in appset-${ENV}.yaml, then
  kubectl apply -f $APPSET
EOF
fi
