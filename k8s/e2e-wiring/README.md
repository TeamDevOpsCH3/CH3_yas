# E2E Wiring — C15 (Hiệp) · Methyl CH3

> Toàn bộ artifact để **YAS storefront chạy E2E thật** trên cụm dev: từ product list, login Keycloak, tới domain sạch. Đây là phần "acceptance gate" của C15 — chứng minh hệ chạy thật, không chỉ pod Running.

## Bối cảnh

Sau khi deploy 13/13 pod dev (C18) + wiring service discovery (C14), storefront vẫn báo **"No Products"** và **không login được**. Truy ngược ra **chuỗi 6 tầng lỗi xếp chồng** — mỗi cái che cái sau. Bộ file này là kết quả gỡ trọn chuỗi đó.

## Chuỗi lỗi & cách gỡ (cho báo cáo C26)

| # | Triệu chứng | Gốc rễ thật | Fix (không sửa source) |
|---|---|---|---|
| 1 | "No Products", `/api/*` trả 404 (Next.js) | Vào thẳng NodePort UI, **bỏ qua gateway** bff | Vào qua bff/ingress, không qua NodePort UI |
| 2 | 500 — bff log `NXDOMAIN 'nginx'` | bff route `/api/**` → host `nginx` (reverse-proxy của docker-compose) mà K8s **chưa deploy** | Deploy lại nginx vào ns dev (`01`, `02`) |
| 3 | nginx CrashLoop `host not found in upstream` | `proxy_pass http://product;` tên thẳng → nginx resolve **lúc startup** → fail | Biến-hoá proxy_pass (resolve lazy runtime) |
| 4 | 502 `product could not be resolved` | nginx `resolver` **không dùng search-domain** → tên ngắn NXDOMAIN | proxy_pass dùng **FQDN** `.dev.svc.cluster.local` |
| 5 | API 200 nhưng `productContent: []` (DB rỗng) | seed JDBC ghi nhầm DB `product` (thiếu `_dev`) — **silent failure** (báo "success") | Override 2 datasource phụ sampledata → `_dev` (`03`) |
| 6a | ảnh `CONNECTION_REFUSED` / login `ERR_NAME_NOT_RESOLVED` | ingress qua NodePort 30080; thiếu ingress Keycloak; domain kèm port | Ingress controller hostPort 80 + ingress identity (`05`, `06`) |
| 6b | Keycloak `Invalid parameter: redirect_uri` | client `storefront-bff` chưa có redirect-uri khớp domain mới | Thêm Valid Redirect URIs + Web Origins trong realm (xem Tech debt) |

**Bài học xuyên suốt:** "deployed/exposed" ≠ "wired/working". 6 tầng độc lập, mỗi cái phải đúng. Cạm bẫy nặng nhất là **#5 silent failure** — seed trả "Insert successfully" trong khi JDBC ghi sai DB.

## Các file

| File | Nội dung |
|---|---|
| `01-nginx-gateway.yaml` | Deployment + Service `nginx` — gateway fan-out `/api/**` → product/cart/... |
| `02-nginx-gateway-configmap.yaml` | Config nginx: resolver CoreDNS + FQDN proxy_pass |
| `03-sampledata-datasource-fix.yaml` | Configmap sampledata trỏ `*_dev` (fix silent seed) |
| `04-ingress-apps.yaml` | 3 Ingress dev (host `.yas.local.com`) |
| `05-ingress-identity.yaml` | Ingress Keycloak (ns keycloak) |
| `06-ingress-controller-setup.md` | Cài controller + hostPort 80 + nodeSelector master + /etc/hosts |
| `apply-all.sh` | Apply đúng thứ tự |

## Cách dùng

```bash
# 1. Cài ingress controller (1 lần) — xem 06-ingress-controller-setup.md
# 2. Apply bộ wiring
./apply-all.sh
# 3. Seed data (nếu product_dev rỗng)
kubectl -n dev run seedtest --image=curlimages/curl --rm -it --restart=Never -- \
  curl -s -X POST http://sampledata.dev.svc.cluster.local:80/sampledata/storefront/sampledata \
  -H 'Content-Type: application/json' -d '{"message":"seed"}'
# 4. Mở http://storefront.yas.local.com/products → login → mua hàng
```

## ⚠️ Tech debt (cần xử trước khi nộp / sau demo)

1. **Redirect-uri sửa TAY trong Keycloak** (qua admin UI) → nằm trong Keycloak DB, KHÔNG versioned. Re-import realm sẽ **mất**. → Cần đưa `http://storefront.yas.local.com/*` (Valid Redirect URIs) + `http://storefront.yas.local.com` (Web Origins) vào `keycloak-yas-realm-import.yaml` của chart Keycloak.
2. **`03-sampledata` + `04-ingress-apps` do Helm quản** (`managed-by: Helm`) → `helm upgrade` ghi đè. Bền hơn: sửa trong Helm values, hoặc re-apply + restart sau mỗi upgrade.
3. **Ảnh thumbnail placeholder** — media service đọc file vật lý từ `/images/` (FileSystemRepository), seed chỉ nạp metadata → 500. Cần mount volume chứa binary + thêm route `/media` vào ingress `api.yas.local.com`. Không chặn E2E product flow (hạng mục storage riêng).
4. **nginx gateway là giải pháp tương thích** (bù con reverse-proxy docker-compose). Hướng clean hơn: sửa gateway-routes của bff trỏ thẳng service thay vì qua nginx (nhưng nhiều route → công hơn).
