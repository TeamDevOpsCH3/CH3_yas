# Hướng dẫn Backup — etcd snapshot + PKI (cert)

## Vì sao cần CẢ HAI?
- **etcd snapshot** = trạng thái cụm (pod-spec, svc, configmap, secret...).
- **PKI (cert)** = chứng chỉ ký ServiceAccount token.
- Restore vào master CŨ (cert còn): chỉ cần etcd. ✅
- Restore vào master MỚI (init lại, cert mới): cần CẢ etcd + pki cũ → token cũ mới khớp cert cũ. Thiếu pki → SA token lệch → auth hỏng.
- → Backup cả hai = restore được mọi tình huống (kể cả mất master).

---

## Trên cụm (tự động) — 2 CronJob

```bash
kubectl apply -f etcd-backup-cronjob.yaml   # etcd snapshot, image etcd, mỗi 6h
kubectl apply -f pki-backup-cronjob.yaml    # pki.tgz, image busybox (có tar), mỗi 6h
kubectl -n kube-system get cronjob          # thấy etcd-backup + pki-backup
```

Cả hai lưu vào `/var/backups/etcd/` trên master:
- `etcd-snapshot.db` (~12MB)
- `pki.tgz` (~vài chục KB)

### Test chạy ngay (không đợi 6h):
```bash
kubectl -n kube-system create job --from=cronjob/etcd-backup etcd-test
kubectl -n kube-system create job --from=cronjob/pki-backup  pki-test
kubectl -n kube-system get jobs
kubectl -n kube-system logs job/pki-test     # "PKI backup saved..."
# kiểm file trên master:
ssh -i ~/.ssh/id_auto root@100.98.171.67 "ls -lh /var/backups/etcd/"
# dọn job test:
kubectl -n kube-system delete job etcd-test pki-test
```

---

## Pull off-cluster về máy (thủ công)

```powershell
powershell -ExecutionPolicy Bypass -File backup-pull.ps1
```
→ Kéo CẢ etcd-snapshot + pki về `D:\...\backups`, đặt tên theo ngày. Dùng `id_auto` nên không hỏi passphrase.

---

## Restore (quy trình tham khảo — ĐỪNG chạy trên cụm thật)

### Vào master CŨ (cert còn) — chỉ etcd:
```bash
etcdutl snapshot restore etcd-snapshot.db --data-dir=/var/lib/etcd-new
# dừng control-plane → thay /var/lib/etcd → khởi động lại
```

### Vào master MỚI (init lại) — cả pki + etcd:
```bash
# 1. Giải nén pki cũ ĐÈ vào trước khi/ngay sau init
tar xzf pki-<date>.tgz -C /etc/kubernetes
# 2. Restore etcd như trên
# → token cũ khớp cert cũ → auth không lệch
```

---

## ⚠️ TEARDOWN sau vấn đáp
| Tài nguyên | Gỡ |
|---|---|
| CronJob etcd-backup + pki-backup | `kubectl -n kube-system delete cronjob etcd-backup pki-backup` |
| Key id_auto trên droplet | xóa khỏi authorized_keys, hoặc xóa droplet |
| **3 droplet + EC2** | **xóa — tốn tiền** |

## ⚠️ KHÔNG commit lên Git
- File key `id_auto` (private key)
- File `pki-*.tgz` (chứa CERT cụm — nhạy cảm!)
- → Thêm `.gitignore`: `*id_auto*`, `*.tgz`, `*.db`, `pki-*`
