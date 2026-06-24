# Kiến trúc cụm K8s — Node, Client & vì sao Tailscale có nhiều máy

> Gửi nhóm Methyl (CH3). Giải thích cụm Kubernetes gồm những máy nào, vì sao trong Tailscale thấy **nhiều `client` và `worker`**, và mỗi máy đóng vai gì.

---

## TL;DR (đọc nhanh)

- **Cụm thật = 3 droplet cloud (DigitalOcean, amd64)** làm "xương sống" — app + hạ tầng chạy ở đây.
- **Máy Windows tham gia cụm qua WSL2**: node K8s thật là môi trường **WSL2** bên trong Windows (tên `worker-*-wsl`), KHÔNG phải bản thân máy Windows.
- **Mỗi laptop Windows xuất hiện 2 lần trong Tailscale**: 1 là host (`client-*`, để gõ kubectl), 1 là WSL2 (`worker-*-wsl`, node thật).
- **2 máy Mac (arm64)** chỉ làm **client** (điều khiển từ xa), KHÔNG làm node.
- Cả 4 laptop đều là **client** → ai cũng vận hành cụm được. Đây là bình thường.

---

## Toàn bộ máy trong Tailscale & vai trò

| Tên trong Tailscale | Máy vật lý | Vai trò | Có chạy pod? |
|---|---|---|---|
| `master-yasdo` | Droplet DO (amd64) | **Node** — control-plane (master) | ✅ |
| `worker-yasdo-1` | Droplet DO (amd64) | **Node** — worker | ✅ |
| `worker-yasdo-2` | Droplet DO (amd64) | **Node** — worker | ✅ |
| `worker-hiep` | WSL2 trong máy Hiệp (amd64) | **Node** — worker mở rộng (on-prem) | ✅ |
| `worker-hoa` | WSL2 trong máy Hòa (amd64) | **Node** — worker mở rộng *(join sau)* | ✅ |
| `client-hiep` | Windows host của Hiệp | **Client** — gõ kubectl (host chứa WSL2) | ❌ |
| `client-hoa` | Windows host của Hòa | **Client** — gõ kubectl (host chứa WSL2) | ❌ |
| `client-cuong` | Mac (arm64) | **Client** — điều khiển từ xa | ❌ |
| `client-hoang` | Mac (arm64) | **Client** — điều khiển từ xa | ❌ |
| `jenkins-ec2` | EC2 | **CI** — build/deploy *vào* cụm | ❌ (không phải node) |

→ Quy tắc nhớ: **`*-yasdo` + `worker-hiep/hoa` = NODE (chạy pod)** · **`client-*` = máy người dùng điều khiển** · **`jenkins-ec2` = CI**.

> **Lưu ý tên khác nhau ở 2 chỗ (cùng 1 máy, không phải lỗi):** trong Tailscale node WSL2 của Hiệp tên `worker-hiep`, nhưng `kubectl get nodes` hiện nó là `laptop-nfigfmr1` (hostname của WSL2). Cùng 1 node, map với nhau qua IP `100.86.181.121`. Tailscale name ≠ kubeadm node name là bình thường.

---

## ❓ Vì sao Tailscale thấy NHIỀU `client` và `worker`?

Câu hỏi rất hay — có 2 lý do:

**(a) Mỗi máy Windows = 2 mục trong Tailscale.**
Node Kubernetes thật chạy trong **WSL2** (một Linux riêng *bên trong* Windows). WSL2 join Tailscale như một máy độc lập. Nên 1 laptop Windows vật lý sinh ra 2 "máy":
- `client-hiep` = **host Windows** (để gõ kubectl, vận hành).
- `worker-hiep` = **WSL2 bên trong** → đây mới là **node cụm thật** (chạy pod).

Cùng 1 cái laptop, nhưng Tailscale thấy 2 entry. Tương tự cho Hòa.

**(b) Có nhiều `client` vì cả 4 laptop đều vận hành cụm được.**
2 Mac + 2 Windows host đều cài kubectl → đều là client. Ai cũng điều khiển cụm từ máy mình (giống cả team đều có quyền thao tác). Đây là cách làm bình thường, không thừa.

→ Tóm lại: **node ít hơn số máy hiện ra**. 4 node thật (`master-yasdo`, `worker-yasdo-1/2`, `worker-hiep`; thêm `worker-hoa` khi Hòa join) — phần còn lại là client/host/CI.

---

## Vì sao Mac KHÔNG làm node?

Mấu chốt là **kiến trúc CPU**, không phải hiệu năng:
- Windows + droplet DO = **amd64**. Mac M-series = **arm64**.
- Image app YAS build trên Jenkins là **amd64-only** → pod app xếp lên node arm64 (Mac) sẽ **`exec format error`**, CrashLoop. K8s không tự dịch image giữa 2 kiến trúc.
- Cố cho Mac làm node thì hoặc phải **build multi-arch** (chậm/rủi ro), hoặc **chặn app khỏi Mac** (node vô nghĩa). Cụm cũng đã đủ RAM → không cần.

→ Mac arm64 = **client** (hữu ích hơn). *Nhưng Windows cũng arm64?* Không — Windows của nhóm là amd64, nên WSL2 trong đó cũng amd64 → làm node được.

---

## Vì sao Windows làm worker (qua WSL2)?

- WSL2 trong Windows = **amd64** → cùng kiến trúc droplet → app chạy được, không cần multi-arch.
- Tạo mô hình **hybrid cloud + on-prem**: cụm xương sống trên cloud, mở rộng node on-prem (laptop). Đây là **điểm cải tiến** khi vấn đáp.
- *Vận hành:* app thật ưu tiên chạy trên **droplet** (cùng region Singapore, độ trễ <1ms, luôn online). Node WSL2 on-prem có độ trễ ~45ms tới cloud + chỉ sống khi WSL2 chạy → dùng để **trình diễn mở rộng**, không gánh app demo chính.

---

## "Client" là gì? Làm được gì?

**Client = máy *điều khiển* cụm từ xa, không *chạy* pod.** Cài `kubectl` + `helm` + có kubeconfig → gõ lệnh trên máy mình, chạy thẳng vào cụm qua Tailscale.

Làm được gần như mọi việc DevOps: `kubectl get/logs/describe/apply`, `helm install/upgrade`, vận hành ArgoCD/Istio, viết & test manifest, debug. Chỉ **không** chạy pod trên chính máy mình (không cần, vì pod chạy trên cụm).

---

## 2 bạn Mac có bị thiệt không? → KHÔNG

- Làm client là **cách làm DevOps chuẩn** — không ai ngồi trực tiếp trên node, đều điều khiển từ xa.
- 2 bạn Mac làm **đầy đủ task**: viết manifest, deploy Helm, test CD, xem log, debug — tất cả qua client.
- Thực tế **cả nhóm** (kể cả Windows) đều dùng client để vận hành cho tiện.

---

## Cách biến máy mình thành client (mỗi người)

1. Cài **kubectl** + **helm**.
2. Xin **kubeconfig** từ Hiệp (chìa khóa cụm — giữ riêng tư, không up Git/chat chung).
3. Bỏ vào `~/.kube/config` (Mac) hoặc `%USERPROFILE%\.kube\config` (Windows).
4. Đảm bảo **Tailscale Connected** (kubeconfig trỏ IP Tailscale master).
5. `kubectl get nodes` → thấy các node **Ready** = thành công.

*(Máy Windows muốn làm thêm **worker** thì theo file `HuongDan_Join_Windows_WSL2.md` — đó là việc khác với làm client.)*

---

## Tóm lại 1 câu

**Node chạy pod = 3 droplet (`*-yasdo`) + WSL2 trong máy Windows (`*-wsl`).** Mọi `client-*` là máy điều khiển (kể cả host Windows chứa WSL2), `jenkins-ec2` là CI. Tailscale thấy nhiều máy vì 1 laptop Windows = host + WSL2, và cả 4 laptop đều làm client — không có gì thừa.
