# Hướng dẫn Backup etcd tự động (2 bản: master + client)

## Thiết kế
- **CronJob** (trong cụm): mỗi 6h tự `etcdctl snapshot save` → ghi đè bản **latest** trên master tại `/var/backups/etcd/etcd-snapshot.db`. Tự động 100% (master luôn bật).
- **Script PowerShell** (client Windows): kéo bản latest về ổ D, đặt tên theo **ngày** → giữ **history** nhiều bản. Off-cluster (Windows ít tắt hơn WSL).

> Vì image etcd không có shell, CronJob chỉ ghi 1 bản "latest" trên master (không timestamp). History do client giữ.

---

## Bước 1 — Apply CronJob (chạy 1 lần, từ WSL hoặc PowerShell có kubectl)

```bash
kubectl apply -f etcd-backup-cronjob.yaml
kubectl -n kube-system get cronjob etcd-backup        # thấy SCHEDULE 0 */6 * * *
```

### Test chạy ngay (không đợi 6h) — tạo 1 job thủ công từ cronjob:
```bash
kubectl -n kube-system create job --from=cronjob/etcd-backup etcd-backup-manual-1
kubectl -n kube-system get jobs                       # etcd-backup-manual-1 Complete
kubectl -n kube-system logs job/etcd-backup-manual-1  # "Snapshot saved..."
```

### Verify file trên master:
```bash
kubectl -n kube-system exec etcd-yas-master -- \
  etcdctl snapshot status /backup/etcd-snapshot.db --write-out=table 2>/dev/null \
  || ssh root@100.98.171.67 "ls -lh /var/backups/etcd/"
```

---

## Bước 2 — Script pull về client (PowerShell)

```powershell
# chạy tay thử
powershell -ExecutionPolicy Bypass -File backup-etcd-pull.ps1
```
→ Ra file `etcd-snapshot-<ngày>.db` trong `D:\...\backups`.

---

## Bước 3 — Tự động hóa pull bằng Task Scheduler (KHÔNG set Expire)

1. Mở **Task Scheduler** (Windows) → **Create Task** (không phải Basic Task).
2. **General:** đặt tên `etcd-backup-pull`. Tick "Run whether user is logged on or not".
3. **Triggers:** New → Daily, hoặc "At log on" (chạy khi mở máy). *(KHÔNG tick Expire — sẽ xóa tay sau vấn đáp.)*
4. **Actions:** New → Program: `powershell.exe`
   Arguments: `-ExecutionPolicy Bypass -File "D:\...\backup-etcd-pull.ps1"`
5. OK.

---

## ⚠️ TEARDOWN — DỌN SAU VẤN ĐÁP (đừng quên!)

| Tài nguyên | Lệnh / cách gỡ |
|---|---|
| Task Scheduler `etcd-backup-pull` | Task Scheduler → chuột phải → **Delete** |
| CronJob `etcd-backup` | `kubectl -n kube-system delete cronjob etcd-backup` (hoặc mất khi xóa cụm) |
| **3 droplet DigitalOcean** | **Xóa trên DO console — KẺO TỐN CREDIT** |
| **EC2 Jenkins agent** | Stop/Terminate trên AWS — KẺO TỐN TIỀN |
| Tailscale | (free, giữ cũng được) hoặc gỡ máy khỏi tailnet |

> Nhắc lại: **droplet + EC2** mới là cái tốn tiền. Task Scheduler/CronJob chỉ là rác nhẹ.
