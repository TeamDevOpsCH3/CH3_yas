# C22 — ArgoCD Application (GitOps CD path) — Methyl CH3

> `develop` → ns `dev` · `main` (sau PR) → ns `staging`. ArgoCD đọc `k8s/charts/`
> trực tiếp qua Application manifest, **KHÔNG chạy script**. Script deploy-yas-*.sh
> là con đường push/bootstrap song song (dùng cho disaster recovery), loại trừ nhau.

## 0. Bộ file

| File | Vai trò |
|---|---|
| `vendor-charts.sh` | Vendor + force-add subchart deps (`charts/*.tgz` + `Chart.lock`) vào Git → ArgoCD render deterministic. **Chạy 1 lần trước tiên.** |
| `app-product-test.yaml` | 1 Application test render 1 service. Verify rồi **xóa**. |
| `appset-dev.yaml` | ApplicationSet 14 service, branch `develop` → ns `dev`. Sync **manual** ban đầu (adopt staged). |
| `appset-staging.yaml` | Mirror, branch `main` → ns `staging`. Auto-sync. **Apply sau khi bootstrap + PR.** |
| `apply-c22.sh` | Bootstrap 1 lệnh: apply appset + wait. `--auto` cho DR. |

Đặt thư mục vào **`k8s/argocd/`** (khớp convention `k8s/charts`, `k8s/deploy`, `k8s/istio`).

---

## 1. Vì sao thiết kế thế này (ADR ngắn — cho C26/vấn đáp)

- **ApplicationSet (list generator) thay vì 28 Application viết tay.** List generator = bảng `SERVICES` keep-list trong `deploy-yas-applications.sh`; template params = các `--set` động. Tức là **bản khai báo của đúng vòng lặp bash** → cùng kết quả, giờ Git-driven + reconciled. Tách 2 ApplicationSet per-env (không matrix env×service) để branch/ns/DB-suffix là hằng số mỗi file → dễ đọc, dễ debug.
- **Multi-source (`$values` ref).** Chart ở `k8s/charts/<svc>/`, value file ở `k8s/deploy/values-<env>.yaml` (khác thư mục). ArgoCD tính `valueFiles` tương đối với path chart → phải dùng source `ref: values` rồi trỏ `$values/k8s/deploy/values-<env>.yaml`. Giữ `affinity` (structure lồng) trong value file thay vì nhồi `--set` cho sạch.
- **Vendored subchart deps commit vào Git.** Chart khai `file://../backend`; deps resolved (`charts/*.tgz`) + `Chart.lock` vốn bị `.gitignore`. ArgoCD pull từ Git → thiếu là render **fail**. Commit deps = Git là source-of-truth **đầy đủ**, reproducible (như commit lockfile).
- **Adopt staged cho dev.** Dev đang chạy 14 service (script deploy). ArgoCD render cùng chart + cùng values → **adopt** không downtime. Bật `selfHeal`/`prune` **chỉ sau khi** `argocd app diff` sạch → tránh ArgoCD sửa/xóa nhầm cụm đang chạy.
- **PR-gate = promotion.** `main` chỉ nhận qua PR + review → auto-sync trên `main` an toàn. Cổng promotion nằm ở PR, không ở sync.
- **Không Image Updater.** Tag pin `7899c97c9956` trong `values-<env>.yaml`. CI build+push+ (sau này) ghi tag vào Git; CD chỉ sync. Image Updater = `[học thêm]`.

---

## 2. Thứ tự chạy — DEV (phiên này)

```bash
# === Trên: worker-hiep WSL2 — repo root ===
cd /mnt/d/HK6/DevOps/Projects/Project02/CH3_yas

# --- B1. Vendor subchart deps + commit (chỉ cần 1 lần) ---
chmod +x k8s/argocd/vendor-charts.sh argocd/apply-c22.sh
./k8s/argocd/vendor-charts.sh
git commit -m "[C22] chore(argocd): vendor subchart deps for GitOps render"
git push origin develop

# --- B2. (1 lần) login argocd CLI để sync/wait ---
ARGO_PW=$(kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d)
argocd login 100.98.171.67:32087 --insecure --username admin --password "$ARGO_PW"
# (32087 = NodePort https của argocd-server; đổi IP node nếu cần)

# --- B3. Smoke test render 1 service (KHÔNG sync) ---
kubectl apply -f k8s/argocd/app-product-test.yaml
sleep 5
argocd app get yas-dev-product-test           # muốn: Sync=OutOfSync, KHÔNG ComparisonError
# Render OK -> xóa app test:
kubectl -n argocd delete application yas-dev-product-test

# --- B4. Apply ApplicationSet dev (staged adopt) ---
./k8s/argocd/apply-c22.sh dev
# Script in ra các bước diff/sync tay tiếp theo. Tóm tắt:
argocd app diff yas-dev-product               # muốn: chỉ khác label helm/argocd
argocd app sync yas-dev-product               # sync thử 1 con
argocd app sync -l env=dev                     # sync cả 14
argocd app wait -l env=dev --health --timeout 900

# --- B5. Sạch hết -> bật auto-reconcile ---
# Mở k8s/argocd/appset-dev.yaml, bỏ comment khối:
#   automated: { prune: true, selfHeal: true }
kubectl apply -f k8s/argocd/appset-dev.yaml
```

### Verify C22 (dev) đạt
```bash
# === Trên: worker-hiep WSL2 ===
argocd app list                                # 14 app yas-dev-*: Synced + Healthy
kubectl -n argocd get applications -l env=dev
```
- [ ] 14 app `yas-dev-*` **Synced + Healthy**, track `develop`.
- [ ] Sửa tay 1 resource (vd `kubectl -n dev scale deploy/product --replicas=2`) → ArgoCD **selfHeal** kéo về đúng Git.
- [ ] Commit nhỏ vào `develop` (vd đổi replica trong chart) → app tự `OutOfSync → Synced`.

---

## 3. Thứ tự chạy — STAGING (sau khi làm xong dev + có RAM)

> ⚠️ 3 tiền đề bắt buộc, thiếu là staging KHÔNG lên được:
> 1. **Config bootstrap:** `./deploy-yas-configuration.sh staging` (tạo `*-application-configmap` chart mount — ns staging đang trống).
> 2. **PR `develop` → `main`:** main phải có `k8s/charts/*` + `values-staging.yaml` + vendored deps. (Đây cũng là **promotion** để demo.)
> 3. **RAM:** staging full ≈ +6-7GB trên nền dev. Apply khi cụm còn chỗ (đừng chạy song song full dev+staging nếu chật).

```bash
# === Trên: worker-hiep WSL2 — repo root ===
# (sau khi merge PR develop->main)
cd k8s/deploy && ./deploy-yas-configuration.sh staging && cd ../..
./k8s/argocd/apply-c22.sh staging --auto           # staging ns trống -> full auto an toàn
```

**Seed data staging:** `sampledata` nằm trong keep-list → tự seed `*_staging` khi sync → product list hiển thị. **Search-in-staging = ngoài scope** (cần Debezium connector thứ 2 → ES index riêng; ES dùng chung). Document staging = "deploy path verified + product seeded; search CDC là dev-only".

---

## 4. CAMS — C22 thể hiện DevOps thế nào (cho báo cáo)

| Trụ | C22 |
|---|---|
| **Culture** | GitOps self-service qua Git; **PR-gate `develop→main` = promotion có review**; owner rõ ràng per-task. |
| **Automation** | ArgoCD **auto-sync + selfHeal + prune** (cụm tự hòa giải, 0 `helm` tay); `vendor-charts.sh` + `apply-c22.sh --auto` (bootstrap 1 lệnh, cắm thẳng vào DR chain). |
| **Measurement** | Trạng thái per-app `Synced/OutOfSync` + `Healthy/Degraded` = tín hiệu phản hồi; **drift detection**. Nối C30 (Grafana/Tempo/Loki) để đo sâu. |
| **Sharing** | Git = single source of truth; manifest declarative ai đọc cũng tái lập; README này + phần ADR. |

---

## 5. Ranh giới & lưu ý

- **ArgoCD vs script:** ArgoCD (pull, đọc charts) và `deploy-yas-*.sh` (push, bootstrap) **loại trừ nhau** — không gọi nhau. ArgoCD replicate logic override của script để ra kết quả giống.
- **Owner:** `deploy-yas-*.sh` gốc là của Hòa (C13), Knight đã refactor → đã báo Hòa đừng đụng song song. `values-*.yaml` do Hiệp (C12/C18).
- **worker-hiep phải sống** (Kafka controller + Debezium). 5 service on-prem (storefront-ui/backoffice-ui/tax/customer/swagger-ui) ưu tiên *mềm* xuống máy Hiệp — máy tắt thì rớt về droplet (không kẹt Pending).
- **k8s/istio/ AuthorizationPolicy đang active** (Hoàng) — có thể chặn traffic khi test; nếu app OutOfSync/Degraded lạ, check policy trước.
