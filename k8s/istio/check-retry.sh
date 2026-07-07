#!/usr/bin/env bash
# check-retry.sh — Verify Istio retry policy is loaded in Envoy runtime.
# Run from repo root: bash k8s/istio/check-retry.sh [namespace]
#
# Checks:
#   1. VirtualServices exist in namespace
#   2. Envoy sidecar of 'order' pod has retry rules loaded (num_retries, retry_on)
set -euo pipefail

NS="${1:-dev}"

echo "=== VirtualServices in ns/$NS ==="
kubectl get virtualservice -n "$NS" 2>/dev/null || { echo "No VirtualServices found."; exit 1; }

echo ""
echo "=== tax-retry spec ==="
kubectl get virtualservice tax-retry -n "$NS" -o jsonpath=\
'{.spec.http[0].retries}' 2>/dev/null \
  | python3 -c "import sys,json; d=json.load(sys.stdin); [print(f'  {k}: {v}') for k,v in d.items()]" \
  || kubectl get virtualservice tax-retry -n "$NS" -o yaml | grep -A6 "retries:"

echo ""
echo "=== Envoy runtime retry config (order → tax) ==="

# Dynamically resolve order pod — no hardcoded name
ORDER_POD=$(kubectl -n "$NS" get pod \
  -l app.kubernetes.io/name=order \
  -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

if [ -z "$ORDER_POD" ]; then
  echo "ERROR: No 'order' pod found in ns/$NS. Is the app deployed?"
  exit 1
fi

echo "  Using pod: $ORDER_POD"

# Check sidecar exists — pod must show 2/2 containers
READY_COUNT=$(kubectl -n "$NS" get pod "$ORDER_POD" \
  --no-headers | awk '{print $2}')
if [ "$READY_COUNT" != "2/2" ]; then
  echo "WARN: Pod $ORDER_POD shows $READY_COUNT (expected 2/2). Sidecar may not be injected."
  echo "      Run: kubectl label namespace $NS istio-injection=enabled --overwrite"
  echo "      Then: kubectl rollout restart deploy/order -n $NS"
else
  echo "  Sidecar ready: $READY_COUNT OK"
fi

# Dump Envoy config — show only our custom retry rule (num_retries=3 + per_try_timeout)
RESULT=$(kubectl -n "$NS" exec "$ORDER_POD" -c istio-proxy -- \
  sh -c 'curl -s http://localhost:15000/config_dump' \
  | grep -A2 '"retry_on": "5xx,connect-failure,reset"')

if echo "$RESULT" | grep -q "num_retries.*3"; then
  echo ""
  echo "$RESULT"
  echo ""
  echo "PASS: Custom retry rule (num_retries=3, retryOn=5xx,connect-failure,reset) loaded in Envoy."
else
  echo ""
  echo "WARN: Custom retry rule not found. Check VirtualService is applied:"
  echo "  kubectl apply -k k8s/istio"
fi
