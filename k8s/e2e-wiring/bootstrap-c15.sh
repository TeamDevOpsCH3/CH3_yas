#!/usr/bin/env bash
# ============================================================================
# bootstrap-c15.sh — Dựng lại toàn bộ C15 E2E wiring sau khi mất cụm.
# Chạy trên: worker-hiep WSL2 (máy có KUBECONFIG).
#
# TIEN DE (phải xong TRƯỚC khi chạy script này):
#   1. Cụm sống: kubectl get nodes -> các node Ready (rebuild theo IaC session backup)
#   2. Hạ tầng + app dev đã deploy: ./cd-deploy.sh dev (C11/C18) -> ns dev 13/13 Running
#
# Script này tự động hoá: ingress controller + hostPort + apply wiring + seed.
# CÒN 2 VIỆC TAY (script sẽ nhắc ở cuối): /etc/hosts client + re-import realm.
# ============================================================================
set -euo pipefail

# --- Cấu hình (đổi nếu cụm khác) ---
MASTER_NODE="yas-master"
MASTER_IP="100.98.171.67"
INGRESS_VERSION="controller-v1.11.3"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

c_green="\033[0;32m"; c_yellow="\033[0;33m"; c_red="\033[0;31m"; c_reset="\033[0m"
log()  { echo -e "${c_green}==>${c_reset} $*"; }
warn() { echo -e "${c_yellow}[!]${c_reset} $*"; }
err()  { echo -e "${c_red}[x]${c_reset} $*" >&2; }

# --- 0. Pre-check: cụm sống + ns dev có app ---
log "[0/6] Pre-check cụm + ns dev"
if ! kubectl get nodes >/dev/null 2>&1; then
  err "Không kết nối được cụm (kubectl get nodes fail). Rebuild cụm trước."
  exit 1
fi
if ! kubectl -n dev get deploy product >/dev/null 2>&1; then
  err "ns dev chưa có app (deploy/product không thấy). Chạy ./cd-deploy.sh dev trước."
  exit 1
fi
ready=$(kubectl -n dev get pods --no-headers 2>/dev/null | grep -c "Running" || true)
log "ns dev: $ready pod Running"

# --- 1. Cài ingress-nginx controller (idempotent) ---
log "[1/6] Cài ingress-nginx controller ($INGRESS_VERSION)"
if kubectl -n ingress-nginx get deploy ingress-nginx-controller >/dev/null 2>&1; then
  warn "Controller đã tồn tại — bỏ qua cài mới."
else
  kubectl apply -f "https://raw.githubusercontent.com/kubernetes/ingress-nginx/${INGRESS_VERSION}/deploy/static/provider/baremetal/deploy.yaml"
  log "Đợi controller deployment xuất hiện..."
  sleep 5
fi

# --- 2. Pin controller về master (hostPort 80/443 đã có sẵn trong manifest baremetal) ---
log "[2/6] Pin controller về $MASTER_NODE (cho hostPort 80 ổn định)"
current_selector=$(kubectl -n ingress-nginx get deploy ingress-nginx-controller \
  -o jsonpath='{.spec.template.spec.nodeSelector.kubernetes\.io/hostname}' 2>/dev/null || true)
if [ "$current_selector" = "$MASTER_NODE" ]; then
  warn "Controller đã pin về $MASTER_NODE — bỏ qua."
else
  kubectl -n ingress-nginx patch deployment ingress-nginx-controller --type='json' -p="[
    {\"op\":\"add\",\"path\":\"/spec/template/spec/nodeSelector\",\"value\":{\"kubernetes.io/hostname\":\"$MASTER_NODE\"}}
  ]"
fi

log "Đợi controller Running 1/1 trên $MASTER_NODE..."
kubectl -n ingress-nginx rollout status deploy/ingress-nginx-controller --timeout=120s

# --- 3. Apply bộ wiring (gọi lại apply-all.sh đã có) ---
log "[3/6] Apply E2E wiring (nginx gateway + ingress + sampledata fix)"
if [ -x "$SCRIPT_DIR/apply-all.sh" ]; then
  "$SCRIPT_DIR/apply-all.sh"
else
  warn "Không thấy apply-all.sh — apply trực tiếp từng file"
  kubectl apply -f "$SCRIPT_DIR/02-nginx-gateway-configmap.yaml"
  kubectl apply -f "$SCRIPT_DIR/01-nginx-gateway.yaml"
  kubectl apply -f "$SCRIPT_DIR/03-sampledata-datasource-fix.yaml"
  kubectl -n dev rollout restart deploy/sampledata
  kubectl apply -f "$SCRIPT_DIR/04-ingress-apps.yaml"
  kubectl apply -f "$SCRIPT_DIR/05-ingress-identity.yaml"
  kubectl -n dev rollout status deploy/nginx --timeout=90s
fi

# --- 4. Đợi sampledata sẵn sàng rồi seed ---
log "[4/6] Đợi sampledata Ready rồi seed data"
kubectl -n dev rollout status deploy/sampledata --timeout=90s
sleep 3
seed_resp=$(kubectl -n dev run seedtest-$$ --image=curlimages/curl --rm -i --restart=Never -- \
  curl -s -X POST http://sampledata.dev.svc.cluster.local:80/sampledata/storefront/sampledata \
  -H 'Content-Type: application/json' -d '{"message":"seed"}' 2>/dev/null || true)
log "Seed response: $seed_resp"

# --- 5. Verify data vào ĐÚNG DB _dev ---
log "[5/6] Verify product_dev có data"
PGPW=$(kubectl -n postgres get secret postgresql -o jsonpath='{.data.postgres-password}' | base64 -d)
pcount=$(kubectl -n postgres exec statefulset/postgresql -- env PGPASSWORD="$PGPW" \
  psql -U postgres -d product_dev -tAc "SELECT count(*) FROM product;" 2>/dev/null | tr -d '[:space:]' || echo "?")
mcount=$(kubectl -n postgres exec statefulset/postgresql -- env PGPASSWORD="$PGPW" \
  psql -U postgres -d media_dev -tAc "SELECT count(*) FROM media;" 2>/dev/null | tr -d '[:space:]' || echo "?")
log "product_dev = $pcount product, media_dev = $mcount media"
if [ "$pcount" = "0" ] || [ "$pcount" = "?" ]; then
  warn "product_dev rỗng! Seed có thể fail — kiểm log: kubectl -n dev logs deploy/sampledata"
fi

# --- 6. Verify ingress trả lời port 80 ---
log "[6/6] Verify ingress port 80 (Keycloak discovery)"
code=$(kubectl -n dev run httptest-$$ --image=curlimages/curl --rm -i --restart=Never -- \
  curl -s -o /dev/null -w "%{http_code}" -H "Host: identity.yas.local.com" \
  "http://$MASTER_IP/realms/Yas/.well-known/openid-configuration" 2>/dev/null || echo "000")
log "Keycloak discovery qua ingress: HTTP $code"

# ============================================================================
echo ""
echo -e "${c_green}=========================================${c_reset}"
echo -e "${c_green}  IN-CLUSTER BOOTSTRAP HOÀN TẤT${c_reset}"
echo -e "${c_green}=========================================${c_reset}"
echo ""
echo -e "${c_yellow}CÒN 2 VIỆC TAY (không tự động hoá — có chủ đích):${c_reset}"
echo ""
echo "  [TAY 1] /etc/hosts trên MÁY CLIENT (máy chạy browser):"
echo "          Chạy setup-hosts.ps1 (Windows, as Admin) — xem file kèm."
echo "          Hoặc thêm tay 4 dòng -> $MASTER_IP:"
echo "            storefront.yas.local.com backoffice.yas.local.com"
echo "            api.yas.local.com identity.yas.local.com"
echo ""
echo "  [TAY 2] Re-import realm (CHỈ nếu redirectUris/webOrigins chưa đúng trong DB):"
echo "          - Realm import CRD KHÔNG tự re-import nếu realm đã tồn tại."
echo "          - Nếu login lỗi 'Invalid redirect_uri': xoá realm Yas trong admin"
echo "            rồi apply lại CRD keycloak-yas-realm-import.yaml, HOẶC sửa tay"
echo "            client storefront-bff (Valid Redirect URIs + Web Origins)."
echo "          - File realm-import ĐÃ versioned đúng (PR #35) nên re-import sẽ chuẩn."
echo ""
echo "  Sau đó mở: http://storefront.yas.local.com/products -> login -> mua hàng"
