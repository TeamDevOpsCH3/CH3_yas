#!/usr/bin/env bash
# apply-all.sh — Apply bộ E2E wiring đúng thứ tự.
# Chạy trên máy có KUBECONFIG (worker-hiep WSL2).
# TIEN DE: ingress-nginx controller da cai + pin master + hostPort 80 (xem 06-*.md).
set -euo pipefail
cd "$(dirname "$0")"

echo "==> [1/5] nginx gateway configmap"
kubectl apply -f 02-nginx-gateway-configmap.yaml

echo "==> [2/5] nginx gateway deployment + service"
kubectl apply -f 01-nginx-gateway.yaml

echo "==> [3/5] sampledata datasource fix (-> *_dev)"
kubectl apply -f 03-sampledata-datasource-fix.yaml
kubectl -n dev rollout restart deploy/sampledata

echo "==> [4/5] ingress apps (storefront/backoffice/swagger)"
kubectl apply -f 04-ingress-apps.yaml

echo "==> [5/5] ingress identity (Keycloak)"
kubectl apply -f 05-ingress-identity.yaml

echo ""
echo "==> Doi nginx gateway Ready..."
kubectl -n dev rollout status deploy/nginx --timeout=90s

echo ""
echo "DONE. Verify:"
echo "  kubectl get ingress -A"
echo "  curl -s -o /dev/null -w '%{http_code}\\n' -H 'Host: identity.yas.local.com' http://100.98.171.67/realms/Yas/.well-known/openid-configuration"
echo "  Browser: http://storefront.yas.local.com/products"
echo ""
echo "Neu product_dev rong, seed:"
echo "  kubectl -n dev run seedtest --image=curlimages/curl --rm -it --restart=Never -- \\"
echo "    curl -s -X POST http://sampledata.dev.svc.cluster.local:80/sampledata/storefront/sampledata \\"
echo "    -H 'Content-Type: application/json' -d '{\"message\":\"seed\"}'"
