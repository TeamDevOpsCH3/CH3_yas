#!/usr/bin/env bash
# ============================================================================
# smoke-test-c15.sh — Quét nhanh sức khoẻ E2E các service trong keep-list 14.
# Chạy trên: worker-hiep WSL2 (có KUBECONFIG).
#
# 3 TẦNG kiểm:
#   [L-API]  gateway noi bo: http://nginx/<svc>/...  -> wiring + service song
#   [L-DOM]  domain ingress:  http://storefront.yas.local.com/api/... -> ingress+DNS
#   [L-SSR]  trang Next.js:    /products, /products/<slug> -> SSR render
#
# PHAN LOAI status (quan trong - tranh bao dong gia):
#   200          = OK
#   401 / 403    = can auth (cart/order/profile) -> DUNG, security hoat dong
#   404          = path sai (can xem lai endpoint, KHONG h¬n la loi service)
#   5xx / 000    = LOI THAT (service chet / wiring hong) -> dieu tra
#
# KHONG test: promotion, rating, recommendation, webhook, payment-paypal
#             (NGOAI keep-list 14 - 000 la by-design)
# ============================================================================
set -uo pipefail

MASTER_IP="100.98.171.67"
c_g="\033[0;32m"; c_y="\033[0;33m"; c_r="\033[0;31m"; c_b="\033[0;36m"; c_x="\033[0m"

# Phan loai + in mau theo code
classify() {
  local code="$1"
  case "$code" in
    2*)        echo -e "${c_g}$code OK${c_x}" ;;
    401|403)   echo -e "${c_g}$code AUTH${c_x}" ;;     # can dang nhap = dung
    404)       echo -e "${c_y}$code PATH?${c_x}" ;;     # path co the sai
    5*|000)    echo -e "${c_r}$code LOI${c_x}" ;;       # loi that
    *)         echo -e "${c_y}$code ?${c_x}" ;;
  esac
}

# curl tu trong cum (qua pod tam) - tra ve HTTP code
curl_in() {
  kubectl -n dev run smk-$$-$RANDOM --image=curlimages/curl --rm -i --restart=Never -- \
    curl -s -o /dev/null -w "%{http_code}" --max-time 8 "$1" 2>/dev/null | tr -dc '0-9' || echo "000"
}

echo -e "${c_b}=========================================${c_x}"
echo -e "${c_b}  SMOKE TEST C15 - keep-list 14 service${c_x}"
echo -e "${c_b}=========================================${c_x}"

# --- TANG 1: API service health (qua gateway nginx noi bo) ---
echo ""
echo -e "${c_b}[L-API] Service health qua gateway noi bo (http://nginx/...)${c_x}"
echo "  service          | health"
echo "  -----------------|--------"
for svc in product cart order customer inventory tax media search; do
  code=$(curl_in "http://nginx/${svc}/actuator/health")
  printf "  %-16s | %b\n" "$svc" "$(classify $code)"
done

# --- TANG 2: Storefront API qua DOMAIN (ingress + CoreDNS rewrite) ---
echo ""
echo -e "${c_b}[L-DOM] Storefront API qua domain (http://storefront.yas.local.com/api/...)${c_x}"
echo "  endpoint                          | code"
echo "  ----------------------------------|------"
declare -A dom_eps=(
  ["product list"]="http://storefront.yas.local.com/api/product/storefront/products?pageNo=0"
  ["categories"]="http://storefront.yas.local.com/api/product/storefront/categories"
  ["cart items (auth)"]="http://storefront.yas.local.com/api/cart/storefront/cart/items"
  ["customer profile (auth)"]="http://storefront.yas.local.com/api/customer/storefront/customer/profile"
)
for name in "${!dom_eps[@]}"; do
  code=$(curl_in "${dom_eps[$name]}")
  printf "  %-33s | %b\n" "$name" "$(classify $code)"
done

# --- TANG 3: SSR pages (Next.js render) ---
echo ""
echo -e "${c_b}[L-SSR] Trang Next.js render (SSR - split-horizon DNS)${c_x}"
echo "  page                  | code"
echo "  ----------------------|------"
for page in "/" "/products" "/products/iphone-15-pro" "/cart"; do
  code=$(curl_in "http://storefront.yas.local.com${page}")
  printf "  %-21s | %b\n" "$page" "$(classify $code)"
done

# --- Backoffice (app thu 2, chua test) ---
echo ""
echo -e "${c_b}[L-BO] Backoffice (app quan tri)${c_x}"
code=$(curl_in "http://backoffice.yas.local.com/")
printf "  %-21s | %b\n" "backoffice home" "$(classify $code)"

echo ""
echo -e "${c_b}=========================================${c_x}"
echo "Ghi chu:"
echo -e "  ${c_g}OK/AUTH${c_x}  = chay tot (AUTH = can login, dung)"
echo -e "  ${c_y}PATH?${c_x}    = 404, endpoint co the khac (kiem tra tay)"
echo -e "  ${c_r}LOI${c_x}      = 5xx/000, dieu tra (vd search = ES infra)"
echo ""
echo "Luu y: smoke-test chi bat loi API/HTTP. Loi SSR/client-side"
echo "(getImageNode, .map of undefined) chi thay tren BROWSER -> test tay (huong A)."
