# YAS K8S Deployment — Methyl CH3

## ⚠️ Deviation vs repo gốc (nashtech-garage/yas)

Nhóm CH3 dùng **Bitnami** thay operator gốc cho data store. Bản operator gốc vẫn lưu tại `*.operator.bak/` làm evidence.

| Component | Repo gốc (nashtech-garage) | Nhóm CH3 | Lý do |
|---|---|---|---|
| PostgreSQL | Zalando Postgres Operator | **Bitnami StatefulSet** | Operator overhead ~500MB RAM |
| Kafka | Strimzi Operator + ZooKeeper | **Bitnami KRaft (no ZK)** | Strimzi ~400MB RAM; Kafka 4.0 bỏ ZK |
| Elasticsearch | ECK Operator | **Bitnami StatefulSet** | ECK overhead ~600MB RAM |
| Keycloak | Keycloak Operator CRD | **Bitnami Chart** | CRD không tương thích K8s version cụm |
| Redis | Bitnami | Bitnami | Giữ nguyên |
| Observability | Grafana/Prometheus/Loki/Tempo | Giữ nguyên | RAM đủ, thầy yêu cầu |
| Access | Ingress + domain `*.yas.local.com` | **NodePort + Tailscale IP** | Không có cloud LoadBalancer |

## Resource references (Bitnami)

- **PostgreSQL:** https://artifacthub.io/packages/helm/bitnami/postgresql
- **Elasticsearch:** https://artifacthub.io/packages/helm/bitnami/elasticsearch
- **Kafka:** https://artifacthub.io/packages/helm/bitnami/kafka
- **Keycloak:** https://artifacthub.io/packages/helm/bitnami/keycloak
- **Redis:** https://artifacthub.io/packages/helm/bitnami/redis
- **Debezium Connect:** https://debezium.io/documentation/reference/stable/operations/kubernetes.html
- **Prometheus:** https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack
- **Grafana:** https://github.com/grafana-operator/grafana-operator
- **Loki:** https://github.com/grafana/loki/tree/main/production/helm/loki
- **Tempo:** https://github.com/grafana/helm-charts/tree/main/charts/tempo
- **Promtail:** https://github.com/grafana/helm-charts/tree/main/charts/promtail
- **OpenTelemetry:** https://github.com/open-telemetry/opentelemetry-operator
## Cluster setup (nhóm CH3)

Nhóm dùng **kubeadm cluster** (3 droplet DigitalOcean + 2 Windows WSL2), kết nối qua **Tailscale** thay vì minikube.

**Prerequisites:**
```shell
# Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# yq (QUAN TRỌNG: script dùng yq để đọc cluster-config.yaml)
sudo snap install yq
# Hoặc: sudo wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
# chmod +x /usr/local/bin/yq

# Verify
helm version
yq --version
kubectl get nodes -o wide
```

## Runbook deploy (đúng thứ tự)

> Chạy tất cả lệnh từ thư mục `k8s/deploy/`

**Bước 1 — Data store + Observability:**
```shell
./setup-cluster.sh
# Hoặc chỉ data store: ./setup-cluster.sh datastore
```

**Bước 2 — Verify infra Running trước khi tiếp:**
```shell
kubectl get pods -n postgres
kubectl get pods -n kafka
kubectl get pods -n elasticsearch
```

**Bước 3 — Keycloak (phụ thuộc PostgreSQL đã Running):**
```shell
./setup-keycloak.sh
kubectl rollout status deploy/identity -n keycloak --timeout=300s
```

**Bước 4 — Redis:**
```shell
./setup-redis.sh
kubectl get pods -n redis
```

**Bước 5 — Deploy config chung:**
```shell
./deploy-yas-configuration.sh baseline
```

**Bước 6 — Deploy toàn bộ app:**
```shell
./deploy-yas-applications.sh baseline
kubectl get pods -n yas
kubectl get svc -n yas
```

## Access (NodePort + Tailscale)

Nhóm CH3 dùng **NodePort** thay Ingress. Lấy Tailscale IP của worker node:

```shell
kubectl get nodes -o wide   # cột INTERNAL-IP → Tailscale IP
```

Sửa `/etc/hosts` trên máy client:
```shell
<TAILSCALE_IP>  storefront.yas.local.com
<TAILSCALE_IP>  backoffice.yas.local.com
<TAILSCALE_IP>  identity.yas.local.com
<TAILSCALE_IP>  api.yas.local.com
```

NodePort cụ thể:
```shell
kubectl get svc -n yas          # xem NodePort từng service
kubectl get svc -n keycloak     # Keycloak NodePort
```
## Keycloak bootstrap admin credentials
The username and password of Keycloak admin user store in the `keycloak-credentials` secret, `keycloak` namespace
use bellow command line to get the admin password
```shell
kubectl get secret keycloak-credentials -n keycloak -o jsonpath="{.data.password}" | base64 --decode
```
bootstrap admin is a temporary admin user. To harden security, create a permanent admin account and delete the temporary one.
## Cluster configuration
All configuration of cluster is setting on [cluster-config.yaml](cluster-config.yaml) in folder k8s-deploy

## Yas configuration 
All configurations of YAS application putted in the yas-configuration helm chart.

Bellow is the values of [values.yaml](../charts/yas-configuration/values.yaml)

## Yas helm charts
All charts of Yas application situated in `charts` folder

To Install the Yas helm charts access to [https://nashtech-garage.github.io/yas/](https://nashtech-garage.github.io/yas/)

## Observability
The Yas observability follow by the standard of Open Telemetry recommendation.
Promtail collect the log from all applications send to Open Telemetry Collector after that, Open Telemetry Collector distribute to Loki server.
The Yas applications also send the metric data to Open Telemetry Collector, Open Telemetry collector send the metric data to Tempo server

View details configuration of Open Telemetry Collector at [opentelemetry](./observability/opentelemetry/values.yaml)

### How to view log on the Grafana
On the left menu select `Expore` -> select `Loki` datasource -> select Label filters:
- namespace
- container (Application)

On the Loki also support track by traceId, on The Tempo you can select the Node graph to view the tracing of request 

---

# 🔧 Methyl CH3 — Điều chỉnh của nhóm (đọc kèm README gốc ở trên)

> Phần trên là README **gốc từ YAS upstream** (minikube + operator). Nhóm Methyl CH3 triển khai trên **hạ tầng khác** (kubeadm hybrid) và **điều chỉnh có chủ đích** vài chỗ. Mục này giải thích **cái gì khác + vì sao**, để đối chiếu khi vận hành/chấm.

## 1. Hạ tầng: kubeadm hybrid thay minikube

README gốc dùng **minikube 1 node**. Nhóm dùng **cụm kubeadm hybrid**:
- 3 droplet cloud (DigitalOcean) + Tailscale VPN — `yas-master`, `yas-worker-1/2`
- 1 node on-prem WSL2 (`worker-hiep`) chạy KUBECONFIG + 1 Kafka controller
- Flannel CNI (iface=tailscale0), ingress-nginx pin master (hostPort 80/443)

→ IP/host trong README gốc (`192.168.49.2`, minikube ip) KHÔNG áp dụng. Client dùng `/etc/hosts` trỏ Tailscale IP master + CoreDNS rewrite cho SSR (xem `k8s/e2e-wiring/`).

## 2. Hạ tầng data store: Bitnami thay Operator

README gốc dùng **operator** (Zalando Postgres, Strimzi Kafka, ECK Elasticsearch, Keycloak Operator). Nhóm chuyển sang **Bitnami Helm charts** (StatefulSet thuần).

**Lý do:** operator overhead ~1.5GB RAM (controller + CRD reconcile). Cụm demo (droplet + WSL2) RAM giới hạn → operator gây OOM. Bitnami nhẹ hơn, đạt cùng mục tiêu.

| Component | README gốc | Nhóm dùng |
|---|---|---|
| PostgreSQL | Zalando operator | Bitnami (`bitnami/postgresql`) |
| Kafka | Strimzi + Zookeeper | Bitnami KRaft (Kafka 4.0, KHÔNG Zookeeper) |
| Elasticsearch | ECK operator | Bitnami (node role master,data,ingest) |
| Keycloak | Keycloak Operator | Bitnami (`bitnamilegacy/keycloak`) |
| Redis | Bitnami | Bitnami (giữ nguyên) |
| Observability | operator (giữ) | operator (GIỮ — theo yêu cầu thầy) |

→ Chi tiết migrate + config Bitnami: xem `HuongDan_Migrate_Deploy_Bitnami.md` (đang thực hiện).

## 3. Deploy đa môi trường (multi-env): dev + staging

README gốc deploy vào **1 namespace `yas`**. Nhóm triển khai **CD đa môi trường** theo branch:

| Branch | Namespace | DB suffix | Tag image |
|---|---|---|---|
| `develop` | `dev` | `*_dev` | develop |
| `main` (sau PR) | `staging` | `*_staging` | main |
| (baseline) | `yas` | gốc | main |

→ `deploy-yas-configuration.sh` + `deploy-yas-applications.sh` giờ **nhận tham số env**:
```bash
./deploy-yas-configuration.sh dev      # cài config vào ns dev
./deploy-yas-applications.sh  dev      # deploy 14 app vào ns dev (DB *_dev)
# tương tự: staging, baseline
```

**Khác biệt so với script gốc:**
- Chỉ deploy **14 service keep-list** (gốc deploy 16 — bỏ location/payment/promotion/rating/recommendation/webhook ngoài phạm vi demo)
- Override image → Docker Hub nhóm (`methylch3/yas-*`)
- DB name theo env (`product_dev`, `product_staging`...)
- Hikari pool = 5/service (3 env chung Postgres, tránh vượt max_connections)
- Vài service ưu tiên (mềm) node on-prem qua `values-onprem.yaml`
- `DRY_RUN=1` để render trước khi apply

Config override nằm trong `values-{baseline,dev,staging,onprem}.yaml` (cùng thư mục `k8s/deploy/`).

## 4. CD tự động: ArgoCD (GitOps)

Ngoài script thủ công trên (dùng để bootstrap / disaster recovery), nhóm dùng **ArgoCD** làm CD chính (pull-based GitOps): ArgoCD watch Git → tự sync `k8s/charts/` theo branch vào ns tương ứng. Script `deploy-yas-*.sh` là con đường **bootstrap/backup** song song, không phải cái ArgoCD gọi.

## 5. Backup / Disaster Recovery

Xem `k8s/ops/` — rebuild droplet (`rebuild-droplets.sh.LOCKED`), etcd + PKI backup CronJob, và `k8s/e2e-wiring/bootstrap-c15.sh` + `bootstrap-search-cdc.sh` (dựng lại E2E wiring + search CDC).

Quy trình rebuild đầy đủ:
```
rebuild-droplets.sh → setup-cluster.sh (Bitnami) → deploy-yas-configuration.sh <env>
→ deploy-yas-applications.sh <env> → bootstrap-c15.sh → bootstrap-search-cdc.sh
```

---
*Section này do Methyl CH3 bổ sung. README gốc giữ nguyên phía trên để đối chiếu bản upstream.*