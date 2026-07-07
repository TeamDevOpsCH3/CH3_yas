# CUTOVER api.dev → in-mesh api-gateway (Option 1)

Chuyển `api.dev.yas.local.com` sang in-mesh nginx api-gateway (originate mТLS →
backend GIỮ STRICT). Gỡ media + swagger PERMISSIVE → còn PERMISSIVE = `gw:8080` + 2 bff.

**THỨ TỰ BẮT BUỘC a → b → c** (gw phải OWN api.dev TRƯỚC khi tắt ingress trực tiếp,
nếu không api.dev/swagger-ui + /media hụt vài giây). Verify smoke sau MỖI bước.

Điều kiện dừng: `verify-deployment.sh` phải **0 FAIL** (WARN "4/5 node" do worker-hoa
cordoned là OK — có chủ đích). Rớt bất kỳ FAIL → ROLLBACK bước đó.

Prereq: `cd k8s/ops && bash verify-deployment.sh` → baseline (0 FAIL). Backup:
`kubectl -n dev get ingress,peerauthentication -o yaml > /tmp/cutover-backup.yaml`

---

## (a) gw OWN api.dev (manual kustomize) — làm TRƯỚC
```bash
kubectl apply -k k8s/api-gateway/          # tạo ingress api.dev + api-test.dev (fallback)
# verify: 8 spec + swagger + media qua gw
for s in product cart order customer inventory tax search media; do
  echo "$s: $(curl -sk --resolve api.dev.yas.local.com:80:<INGRESS_IP> -o /dev/null -w %{http_code} http://api.dev.yas.local.com/$s/v3/api-docs)"
done
curl -sk --resolve api.dev.yas.local.com:80:<INGRESS_IP> -o /dev/null -w "swagger:%{http_code}\n" http://api.dev.yas.local.com/swagger-ui/
cd k8s/ops && bash verify-deployment.sh    # phải 0 FAIL
```
Lúc này gw + swagger-ui/media direct CÙNG tồn tại (longest-prefix: /swagger-ui,/media
→ direct; /<svc> → gw). An toàn.
**Rollback (a):** `kubectl -n dev delete ingress api-gateway` (giữ api-gateway-test).

## (b) push + ArgoCD sync — tắt ingress trực tiếp swagger/media + swagger URLS relative
```bash
git push origin develop
# ArgoCD sync (hoặc: argocd app sync yas-dev-swagger-ui yas-dev-media)
#   -> swagger-ui ingress.enabled=false, media backend.ingress.enabled=false
#   -> swagger URLS = relative /<svc>/v3/api-docs
# Giờ /swagger-ui + /media chỉ còn qua gw (path / catch-all).
kubectl -n dev get ingress    # chỉ còn api-gateway (+ storefront/backoffice-bff); swagger-ui/media INGRESS biến mất
# verify:
curl -sk --resolve api.dev.yas.local.com:80:<INGRESS_IP> -o /dev/null -w "swagger:%{http_code}\n" http://api.dev.yas.local.com/swagger-ui/
curl -sk --resolve api.dev.yas.local.com:80:<INGRESS_IP> -o /dev/null -w "media:%{http_code}\n" http://api.dev.yas.local.com/media/medias/7/file/iphone15_thumbnail.jpg
cd k8s/ops && bash verify-deployment.sh    # phải 0 FAIL
# browser: api.dev/swagger-ui/ -> dropdown 8 service load spec (relative -> gw), 0 mixed-content
```
**Rollback (b):** ArgoCD sync về commit trước (revert swagger-ui/media ingress.enabled)
→ direct ingress trở lại. gw vẫn chạy song song (vô hại).

## (c) gỡ media/swagger PERMISSIVE (manual kustomize) — làm CUỐI
```bash
kubectl apply -k k8s/istio/                       # kustomization đã bỏ 2 PeerAuth
kubectl -n dev delete peerauthentication media-ingress swagger-ui-ingress   # xoá runtime
kubectl -n dev get peerauthentication             # còn: default, *-bff-ingress, api-gateway-ingress
# verify: media/swagger vẫn 200 (giờ qua gw mТLS, không cần PERMISSIVE)
curl -sk --resolve api.dev.yas.local.com:80:<INGRESS_IP> -o /dev/null -w "media:%{http_code} swagger:" http://api.dev.yas.local.com/media/medias/7/file/iphone15_thumbnail.jpg
cd k8s/ops && bash verify-deployment.sh    # phải 0 FAIL
```
**Rollback (c):** `kubectl apply -f k8s/istio/peer-authentication-media-ingress.yaml -f k8s/istio/peer-authentication-swagger-ingress.yaml` (file còn nguyên) + uncomment 2 dòng trong `k8s/istio/kustomization.yaml`.

---

## Kết quả cuối
- PERMISSIVE: `api-gateway:8080` + `storefront-bff:80` + `backoffice-bff:80` = **3** (media+swagger gỡ). **12 backend STRICT tuyệt đối.**
- api.dev: gw ôm /swagger-ui + /media + /<svc>/v3/api-docs. swagger dropdown 8 service (relative → gw → mТLS). 0 mixed-content.
- `api-gateway-test` (api-test.dev) GIỮ làm fallback → xoá sau khi ổn định (`kubectl -n dev delete ingress api-gateway-test` + xoá `ingress-test.yaml` khỏi kustomization).

## Lưu ý
- `<INGRESS_IP>` = IP ingress-nginx (vd 100.98.171.67).
- Kiali: kiểm 8 backend mТLS lock (khoá), 0 backend PERMISSIVE.
- dev chỉ 8/12 backend deployed; rating/promotion/location/payment 404 ở dropdown (chưa deploy) — bình thường.
- Multi-env: staging giữ nguyên tới khi có gw staging (thay đổi này per-env dev).
