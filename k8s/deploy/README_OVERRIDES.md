# C12 + C18 — Override Helm values & Namespace (Methyl CH3 · YAS CD)

Bộ override để **tái dùng chart YAS gốc** (không sửa source) cho 3 môi trường, trỏ image về
Docker Hub `methylch3/yas-*`, tách app theo namespace, dùng chung 1 bộ hạ tầng.

## Cấu trúc
```
k8s/overrides/
├── cd-deploy.sh            # 1 script chạy cho cả baseline/dev/staging (C12 + C18)
├── values-baseline.yaml    # ns yas     · tag main    · DB gốc
├── values-dev.yaml         # ns dev     · tag develop · DB *_dev   · pullPolicy Always
├── values-staging.yaml     # ns staging · tag main    · DB *_staging
├── ns-dev-staging.yaml      # 2 Namespace dev & staging
└── README_OVERRIDES.md
```
Đặt thư mục này tại `k8s/overrides/` trong repo (cạnh `k8s/charts/`).

## Khác biệt cốt lõi so với plan v1.0 (đã sửa)
1. **Key override đúng là `backend.image.*` / `ui.image.*`** (không phải `image.*` top-level) —
   vì chart bọc trong subchart `backend`/`ui`. `swagger-ui` dùng image official `swaggerapi/swagger-ui`
   → **không** override image.
2. **KHÔNG `nodeSelector: arch=amd64`** — cụm đồng nhất amd64 (droplet + WSL2). Chỉ còn tùy chọn
   *weighted affinity* ưu tiên droplet (đã để sẵn comment trong `values-dev.yaml`).
3. **Hạ tầng đặt theo ns từng component** (`postgres`, `kafka`, `keycloak`, `elasticsearch`, `redis`)
   khớp endpoint built-in trong `yas-configuration` (`postgresql.postgres`,
   `kafka-cluster-kafka-brokers.kafka`...) → **0 dòng override endpoint**, app ở mọi ns gọi
   cross-namespace `<svc>.<ns>` là chạy.

## Bố trí node (demo hybrid)
- **9 service nặng**: affinity **mềm** ưu tiên droplet (`node-type=cloud`, weight 80).
- **5 service nhẹ** (`storefront-ui`, `backoffice-ui`, `tax`, `customer`, `swagger-ui`): affinity **mềm**
  ưu tiên **pool on-prem** (`node-type=onprem`, weight 100, qua overlay `values-onprem.yaml`).
  - 1 hoặc cả 2 máy on-prem (Hiệp `laptop-nfigfmr1` + Hòa `worker-hoa`) **BẬT** → scheduler tự **rải**
    5 pod này lên pool on-prem (minh chứng hybrid).
  - Cả 2 máy on-prem **TẮT** → preference không khớp → 5 pod **tự lên droplet**, web KHÔNG sập, không kẹt Pending.
- ⚠️ **Lúc deploy nên bật CẢ 2 máy on-prem** để tải chia đều; nếu chỉ 1 máy bật, 5 pod dồn về máy đó
  (pull 5 image cùng lúc → có thể nghẽn I/O). Bật cả 2 thì mỗi máy ~2-3 image, nhẹ.
- Đổi danh sách: `ONPREM="storefront-ui tax" ./deploy-yas-applications.sh baseline`.

## Chạy
```bash
cd k8s/overrides

# (1 lần) gắn nhãn node: 3 droplet = cloud, 2 máy on-prem = onprem:
kubectl label node yas-master yas-worker-1 yas-worker-2 node-type=cloud --overwrite
kubectl label node laptop-nfigfmr1 worker-hoa node-type=onprem --overwrite
kubectl get nodes -L node-type            # kiểm nhãn đúng

# C12 — baseline vào ns yas
./deploy-yas-applications.sh baseline

# C18 — tạo ns rồi deploy dev/staging
kubectl apply -f ns-dev-staging.yaml
./deploy-yas-applications.sh dev
./deploy-yas-applications.sh staging
```
Script tự cài `yas-configuration` (ConfigMap/Secret dùng chung) vào từng ns trước khi deploy app,
tắt `serviceMonitor` (cụm không có Prometheus Operator), và set affinity/nodeSelector như trên.

## Verify (checklist C12)
```bash
# 1) Render KHÔNG apply — xác nhận image trỏ Docker Hub, không còn ghcr.io:
DRY_RUN=1 ./deploy-yas-applications.sh dev | grep -E 'image:|SPRING_DATASOURCE_URL' | sort -u
#   → image: "methylch3/yas-product:develop"   (… mọi service)
#   → value: jdbc:postgresql://postgresql.postgres:5432/product_dev

# 2) Sau apply:
kubectl -n dev get pods -o wide          # Running, rải trên droplet (+ on-prem nếu bật)
kubectl -n dev get deploy -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.template.spec.containers[0].image}{"\n"}{end}'
kubectl -n dev exec deploy/product -- sh -c 'echo $SPRING_DATASOURCE_URL'   # …/product_dev
```
- [ ] image render ra `methylch3/...` (không còn `ghcr.io`)
- [ ] tag đúng: dev=`develop`, staging/baseline=`main`
- [ ] không file nào trong `k8s/charts/` bị sửa
- [ ] app ns dev/staging kết nối được hạ tầng dùng chung qua FQDN cross-namespace

## ⚠️ Việc Hòa cần làm trước (C11) để Q2 (tách DB theo env) chạy thật
Postgres operator (Zalando) chỉ tạo sẵn DB **không hậu tố**. Muốn cô lập dev/staging bằng database
riêng thì phải thêm vào `spec.databases:` của CR (`k8s/deploy/postgres/postgresql/templates/postgresql.yaml`):
```yaml
  databases:
    # ... giữ nguyên list cũ, BỔ SUNG search/sampledata (đang thiếu cả ở baseline) + biến thể env:
    search:            yasadminuser
    sampledata:        yasadminuser
    product_dev:       yasadminuser
    cart_dev:          yasadminuser
    order_dev:         yasadminuser
    customer_dev:      yasadminuser
    inventory_dev:     yasadminuser
    tax_dev:           yasadminuser
    media_dev:         yasadminuser
    search_dev:        yasadminuser
    sampledata_dev:    yasadminuser
    product_staging:   yasadminuser
    cart_staging:      yasadminuser
    order_staging:     yasadminuser
    customer_staging:  yasadminuser
    inventory_staging: yasadminuser
    tax_staging:       yasadminuser
    media_staging:     yasadminuser
    search_staging:    yasadminuser
    sampledata_staging: yasadminuser
```
- **CDC/eventuate slot:** `order` có logical slot cho CDC. Nếu demo dev/staging có chạy luồng CDC của
  order thì thêm slot tương ứng cho `order_dev`/`order_staging`; nếu không, bỏ qua (không ảnh hưởng demo).
- ES indices & Redis keyspace vẫn **dùng chung** giữa dev/staging (chấp nhận trong phạm vi đồ án; ghi vào báo cáo).

## Bắc cầu sang C22 (ArgoCD)
Mỗi service = 1 ArgoCD `Application`. `image.repository` (khác nhau từng service) đưa vào
`spec.source.helm.parameters` của Application; phần env-common dùng
`spec.source.helm.valueFiles: [overrides/values-<env>.yaml]`. Cùng đúng giá trị script đang `--set`.

## Còn treo
- **payment**: không trong list GIỮ → chưa deploy. Chờ thầy xác nhận `order→payment`.
  Nếu phải giữ: thêm dòng `payment backend yas-payment payment` vào bảng trong `cd-deploy.sh`.
- **Expose frontend**: C15 dùng NodePort + `/etc/hosts` (không Ingress). Host ingress script set sẵn
  chỉ là metadata; cần ingress controller mới có tác dụng — để C14/C15 quyết.
