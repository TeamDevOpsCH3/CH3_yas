#!/usr/bin/env bash
# =============================================================================
# vendor-charts.sh — Vendor Helm subchart dependencies into Git for ArgoCD.
# Methyl CH3 — YAS CD (C22).
#
# WHY: each k8s/charts/<svc>/ declares its subchart via `file://../backend`
# (or ../ui). The resolved dependency (charts/*.tgz) + Chart.lock are currently
# .gitignore'd. ArgoCD repo-server clones the repo from GIT and renders Helm
# there — if the vendored deps are missing, render fails (ComparisonError).
# Committing the resolved deps makes Git a COMPLETE source of truth =
# deterministic, reproducible renders (GitOps / IaC principle — like a lockfile).
#
# WHAT: for each keep-list chart -> `helm dependency update` -> force-add the
# produced Chart.lock + charts/*.tgz (force = bypass .gitignore). Does NOT commit;
# you review `git status` then commit yourself.
#
# RUN ON: worker-hiep WSL2, from the repo root:
#   cd /mnt/d/HK6/DevOps/Projects/Project02/CH3_yas
#   ./k8s/argocd/vendor-charts.sh
# =============================================================================
set -euo pipefail

CHARTS_DIR="k8s/charts"

# Same 14 keep-list as deploy-yas-applications.sh (SERVICES table).
SERVICES="product cart order customer inventory tax media search sampledata \
storefront-bff backoffice-bff storefront-ui backoffice-ui swagger-ui"

[ -d "$CHARTS_DIR" ] || { echo "ERROR: run from repo root (missing $CHARTS_DIR)"; exit 1; }

echo "==> Vendoring subchart dependencies for ArgoCD (14 keep-list charts)"
for svc in $SERVICES; do
  chart="$CHARTS_DIR/$svc"
  if [ ! -d "$chart" ]; then
    echo "  -- skip $svc (no chart dir)"
    continue
  fi
  if ! grep -q '^dependencies:' "$chart/Chart.yaml" 2>/dev/null; then
    echo "  -- skip $svc (no dependencies block)"
    continue
  fi
  echo "  >> helm dependency update $svc"
  helm dependency update "$chart" >/dev/null
  # force-add resolved deps + lock so they land in Git despite .gitignore
  git add -f "$chart/Chart.lock" 2>/dev/null || true
  git add -f "$chart"/charts/*.tgz 2>/dev/null || true
done

echo
echo "==> Staged (force-added) files:"
git status --short -- "$CHARTS_DIR" | grep -E '\.(tgz)$|Chart\.lock' || echo "  (nothing staged — check output above)"
echo
echo "Next (run yourself):"
echo "  git commit -m \"[C22] chore(argocd): vendor subchart deps for GitOps render\""
echo "  git push origin develop"
