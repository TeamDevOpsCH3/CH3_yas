# Kiến trúc cụm K8s — Vì sao Windows làm worker, Mac làm client

> Gửi nhóm Methyl (CH3). Tài liệu giải thích cách 4 laptop + cloud tham gia vào cụm Kubernetes, và vì sao mỗi máy đóng vai khác nhau.

---

## TL;DR (đọc nhanh)

- **Cụm thật chạy trên 3 droplet cloud DigitalOcean** (đều amd64) — đây là "xương sống", app chạy ở đây.
- **2 máy Windows (amd64)** → join làm **worker mở rộng** (node phụ).
- **2 máy Mac (Apple Silicon = arm64)** → làm **client**, KHÔNG làm node.
- Lý do gốc: **kiến trúc CPU** (amd64 vs arm64), không phải Mac "yếu" hay bị loại.
- **2 bạn Mac vẫn làm đầy đủ mọi task** — qua client (giải thích bên dưới).

---

## Cụm gồm những gì

| Thành phần | Máy | Vai trò |
|---|---|---|
| `master-yasdo` + `worker-yasdo-1/2` | 3 droplet DO (amd64) | **Cụm chính** — app + hạ tầng chạy ở đây |
| `worker-hiep`, `worker-hoa` | 2 Windows (amd64) | **Worker mở rộng** (node phụ, join sau) |
| `client-cuong`, `client-hoang` | 2 Mac (arm64) | **Client** — điều khiển cụm từ xa |
| `jenkins-ec2` | EC2 | **CI** — build/deploy *vào* cụm, KHÔNG phải node |

---

## Vì sao 2 máy Mac KHÔNG làm node (worker)?

Mấu chốt là **kiến trúc CPU**, không phải hiệu năng:

- Máy Windows + droplet DO = **amd64 (x86_64)**.
- Máy Mac M-series = **arm64 (Apple Silicon)** — CPU khác hẳn.
- Image app YAS được build trên Jenkins (EC2) là **amd64-only**. Một pod app amd64 nếu bị xếp lên node arm64 (Mac) sẽ **không chạy được** — lỗi `exec format error`, pod cứ CrashLoopBackOff. Kubernetes **không tự dịch** image giữa 2 kiến trúc.

→ Nếu cố cho Mac làm node, chỉ có 2 đường, đường nào cũng dở:
1. **Build image multi-arch** (amd64 + arm64) cho cả ~13 service Java → cross-build arm64 rất chậm + dễ lỗi trên agent CI yếu. Không đáng, nhất là sát deadline.
2. **Ép app không chạy trên Mac** (nodeAffinity) → Mac làm node nhưng không gánh app được gì → làm node vô nghĩa.

→ Cộng thêm: cụm 3 droplet đã có **~24GB RAM**, dư cho workload (~12–16GB). **Không cần RAM của Mac.** Nên cho Mac làm node = thêm phức tạp (arch) + rủi ro mà chẳng được lợi gì.

**Kết luận:** Mac arm64 → **không làm node chạy app**. Để Mac làm **client** (hữu ích hơn nhiều, xem dưới).

---

## Vì sao 2 máy Windows làm worker?

- **amd64** → cùng kiến trúc với droplet → app chạy được, **không cần** build multi-arch.
- Tạo mô hình **"hybrid cloud + on-prem"**: cụm xương sống trên cloud, mở rộng thêm node on-prem (laptop nhóm). Đây là **điểm cải tiến** so với các nhóm chỉ chạy trên laptop hoặc chỉ 1 mô hình — ăn điểm khi vấn đáp.
- *Lưu ý vận hành:* app thật vẫn ưu tiên chạy trên **droplet** (ổn định, luôn online). Windows-worker dùng để **trình diễn cơ chế mở rộng** (cụm có thể scale ra máy on-prem khi cần). Không để service demo chính phụ thuộc laptop (laptop có thể sleep/tắt).

---

## "Client" là gì? Làm được gì?

**Client = máy để *điều khiển* cụm từ xa, không *chạy* cụm.**

Cụ thể: trên máy (Mac hay Windows), bạn cài `kubectl` + `helm`, lấy file **kubeconfig** (Hiệp phát) về máy. Từ đó gõ lệnh **ngay trên máy mình**, lệnh chạy thẳng vào cụm droplet qua mạng Tailscale.

So sánh cho dễ hiểu:
- **Node** (droplet, Windows-worker) = máy *thực sự chứa và chạy* pod.
- **Client** (Mac) = máy bạn ngồi gõ lệnh để *ra lệnh* cho cụm — giống điều khiển server từ xa. Bản thân client **không chứa pod nào**.

**Trên client bạn làm được (gần như mọi việc DevOps):**
- `kubectl get pods / nodes / svc`, xem log (`kubectl logs`), mô tả (`kubectl describe`), debug.
- `kubectl apply -f ...`, `helm install / upgrade` → deploy & cập nhật service.
- Vận hành ArgoCD, Istio/Kiali qua CLI.
- Viết & test manifest/Helm chart, kiểm tra pipeline.

**Không làm được:** chạy pod *trên chính máy mình* (vì máy không phải node) — nhưng điều này không cần thiết, vì pod chạy trên cụm rồi.

---

## 2 bạn Mac có bị thiệt không? → KHÔNG

- Làm client **không phải bị loại** — đây là **cách làm DevOps chuẩn thực tế**: hầu như không ai ngồi trực tiếp trên node để làm việc, tất cả đều điều khiển cụm từ xa qua kubectl/kubeconfig.
- 2 bạn Mac vẫn làm **đầy đủ task của mình**: viết manifest, deploy bằng Helm, test CD, xem log, debug service... — tất cả qua client.
- Thực tế **cả nhóm (kể cả Windows)** đều nên dùng client để vận hành cho tiện, thay vì SSH vào droplet mỗi lần.

---

## Cách biến máy mình thành client (mỗi người tự làm)

1. Cài **kubectl** + **helm** trên máy.
2. Xin **kubeconfig** từ Hiệp (đây là chìa khóa truy cập cụm — giữ riêng tư, không chia sẻ công khai).
3. Bỏ kubeconfig vào `~/.kube/config` (Mac/Linux) hoặc `%USERPROFILE%\.kube\config` (Windows).
4. Đảm bảo **Tailscale đang Connected** (kubeconfig trỏ vào IP Tailscale của master).
5. Test: `kubectl get nodes` → thấy 3 node droplet **Ready** là thành công — bạn đã điều khiển được cụm từ máy mình.

---

## Tóm lại 1 câu

**Droplet = cụm (app chạy) · Windows = worker mở rộng (amd64, demo hybrid) · Mac = client (điều khiển từ xa, vì arm64 không chạy được image app) · EC2 = CI.** Mỗi máy một vai đúng thế mạnh — không ai thừa, không ai thiệt.
