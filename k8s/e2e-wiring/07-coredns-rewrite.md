# CoreDNS Rewrite — SSR Split-Horizon DNS (tầng 7)

> Mắt xích DNS để **trang detail (SSR) chạy**. Thiếu nó: list OK nhưng detail 500.

## Vấn đề: split-horizon DNS

Storefront có 2 kiểu render:
- **List** (`/products`): fetch **client-side** (browser) → browser có `/etc/hosts` → resolve `storefront.yas.local.com` OK.
- **Detail** (`/products/[slug]`): render **server-side (SSR)** → pod `storefront-ui` TỰ fetch `http://storefront.yas.local.com/api` lúc render.

Pod **KHÔNG có** `/etc/hosts` → `getaddrinfo ENOTFOUND storefront.yas.local.com` → SSR crash → **500**.

→ Cùng 1 hostname, 2 đường resolve khác nhau:
- Client (browser): qua `/etc/hosts` → master IP → ingress
- Pod (in-cluster): qua **CoreDNS** → cần rewrite

## Fix: CoreDNS rewrite

Thêm rewrite vào CoreDNS Corefile cho pod resolve `*.yas.local.com` → service nội bộ:

```
rewrite name identity.yas.local.com   identity.keycloak.svc.cluster.local   # có sẵn (C14 - Hoàng)
rewrite name storefront.yas.local.com storefront-bff.dev.svc.cluster.local  # thêm (C15, legacy non-.dev)
rewrite name backoffice.yas.local.com backoffice-bff.dev.svc.cluster.local  # thêm (C15, legacy non-.dev)
rewrite name api.yas.local.com        swagger-ui.dev.svc.cluster.local      # thêm (C15, legacy non-.dev)
# --- .dev per-env: API_BASE_PATH/publicUrl dùng *.dev.yas.local.com -> SSR (getServerSideProps)
#     trong pod phải resolve được, nếu thiếu -> ENOTFOUND -> product detail 500 ---
rewrite name storefront.dev.yas.local.com storefront-bff.dev.svc.cluster.local
rewrite name backoffice.dev.yas.local.com backoffice-bff.dev.svc.cluster.local
rewrite name api.dev.yas.local.com        swagger-ui.dev.svc.cluster.local
# --- .staging per-env: target NAMESPACE = staging (không phải dev) ---
#     main -> ns staging; SSR staging cũng cần resolve *.staging trong pod, thiếu -> 500
rewrite name storefront.staging.yas.local.com storefront-bff.staging.svc.cluster.local
rewrite name backoffice.staging.yas.local.com backoffice-bff.staging.svc.cluster.local
rewrite name api.staging.yas.local.com        swagger-ui.staging.svc.cluster.local
```

## Cách áp dụng (KHÔNG apply đè thô — phải merge)

CoreDNS configmap là cấu hình DNS TOÀN CỤM. Đừng apply file cứng đè lên (sẽ mất config khác). Cách an toàn — chèn vào Corefile hiện có:

```bash
# Backup
kubectl -n kube-system get configmap coredns -o yaml > /tmp/coredns-backup.yaml

# Lấy Corefile, chèn 3 rewrite sau dòng identity
kubectl -n kube-system get configmap coredns -o jsonpath='{.data.Corefile}' > /tmp/Corefile.cur
sed -i '/rewrite name identity.yas.local.com/a\        rewrite name storefront.yas.local.com storefront-bff.dev.svc.cluster.local\n        rewrite name backoffice.yas.local.com backoffice-bff.dev.svc.cluster.local\n        rewrite name api.yas.local.com swagger-ui.dev.svc.cluster.local\n        rewrite name storefront.dev.yas.local.com storefront-bff.dev.svc.cluster.local\n        rewrite name backoffice.dev.yas.local.com backoffice-bff.dev.svc.cluster.local\n        rewrite name api.dev.yas.local.com swagger-ui.dev.svc.cluster.local\n        rewrite name storefront.staging.yas.local.com storefront-bff.staging.svc.cluster.local\n        rewrite name backoffice.staging.yas.local.com backoffice-bff.staging.svc.cluster.local\n        rewrite name api.staging.yas.local.com swagger-ui.staging.svc.cluster.local' /tmp/Corefile.cur

# Apply + reload
kubectl -n kube-system create configmap coredns --from-file=Corefile=/tmp/Corefile.cur \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n kube-system rollout restart deploy/coredns
```

(bootstrap-c15.sh đã tích hợp bước này, idempotent — chỉ thêm nếu chưa có.)

## Verify (BẮT BUỘC — CoreDNS là DNS toàn cụm)

```bash
kubectl -n dev run dnstest --image=busybox:1.36 --rm -it --restart=Never -- sh -c '
  nslookup product.dev.svc.cluster.local       # PHẢI resolve (DNS cụm không vỡ)
  nslookup storefront.yas.local.com            # rewrite -> ClusterIP storefront-bff
'
```

## Rollback (nếu DNS cụm vỡ)

```bash
kubectl -n kube-system apply -f /tmp/coredns-backup.yaml
kubectl -n kube-system rollout restart deploy/coredns
```

## Lưu ý

- `api.yas.local.com` rewrite tới `swagger-ui` — đúng cho swagger. Nếu SSR cần ảnh từ `api.yas.local.com/media`, sẽ cần rewrite riêng tới `media` (nhưng media `/images/` đang trống nên ảnh placeholder — chưa cần).
- Rewrite này song song với `/etc/hosts` client: client dùng hosts (→ ingress port 80), pod dùng CoreDNS rewrite (→ service ClusterIP). Hai đường độc lập, cùng tới đích.
