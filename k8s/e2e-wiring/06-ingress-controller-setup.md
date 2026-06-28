# Ingress-nginx Controller — Setup (L4: domain sạch port 80)

> Controller KHÔNG phải manifest tự chứa (cài từ URL upstream) nên ghi lại các bước ở đây để tái lập sau rebuild.

## 1. Cài controller (bản baremetal — NodePort, hợp cụm kubeadm tự dựng)

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/baremetal/deploy.yaml
```

Đợi controller Running + 2 job admission Completed:

```bash
kubectl -n ingress-nginx get pods
```

## 2. Pin controller về master + dùng hostPort 80/443

Mặc định controller expose NodePort random (vd 32355). Để domain SẠCH không port, ép controller chạy trên master (luôn online) + bind thẳng port 80/443 host:

```bash
kubectl -n ingress-nginx patch deployment ingress-nginx-controller --type='json' -p='[
  {"op":"add","path":"/spec/template/spec/nodeSelector","value":{"kubernetes.io/hostname":"yas-master"}}
]'
```

Lưu ý: bản baremetal đã set sẵn `hostPort: 80/443` trong container ports, nên chỉ cần thêm `nodeSelector` để pin về master. Verify controller chạy đúng node:

```bash
kubectl -n ingress-nginx get pod -o wide | grep controller   # NODE = yas-master, Running 1/1
```

## 3. /etc/hosts trên máy client (Windows — máy chạy browser)

Trỏ các domain về Tailscale IP master (100.98.171.67):

```
100.98.171.67  storefront.yas.local.com
100.98.171.67  backoffice.yas.local.com
100.98.171.67  api.yas.local.com
100.98.171.67  identity.yas.local.com
```

PowerShell (Run as Admin):

```powershell
Add-Content "$env:windir\System32\drivers\etc\hosts" "`n100.98.171.67 storefront.yas.local.com backoffice.yas.local.com api.yas.local.com identity.yas.local.com"
ipconfig /flushdns
```

## 4. Verify

```bash
# Keycloak discovery qua domain port 80 -> 200
curl -s -o /dev/null -w "%{http_code}\n" -H "Host: identity.yas.local.com" \
  http://100.98.171.67/realms/Yas/.well-known/openid-configuration
```

## Vì sao hostPort thay vì NodePort?

- NodePort kèm port cao (30080) → URL phải có `:30080` → ảnh/issuer config (host không port) gọi port 80 → `CONNECTION_REFUSED`.
- hostPort 80 → domain sạch không port → khớp config app (issuer-uri, media publicUrl đều host không port).
- Đánh đổi: hostPort bind theo node controller chạy → phải pin `nodeSelector` về master (ổn định, luôn online).
