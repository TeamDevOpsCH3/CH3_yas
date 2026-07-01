# YAS K8S Deployment
## Resource cluster installation reference
- **Postgresql:** https://github.com/zalando/postgres-operator
- **Elasticsearch:** https://github.com/elastic/cloud-on-k8s
- **Kafka:** https://github.com/strimzi/strimzi-kafka-operator
- **Debezium Connect:** https://debezium.io/documentation/reference/stable/operations/kubernetes.html
- **Keycloak:** https://www.keycloak.org/operator/installation
- **Redis:** https://artifacthub.io/packages/helm/bitnami/redis
- **Reloader:** https://github.com/stakater/Reloader
- **Prometheus:** https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack
- **Grafana:** https://github.com/grafana-operator/grafana-operator
- **Loki:** https://github.com/grafana/loki/tree/main/production/helm/loki
- **Tempo:** https://github.com/grafana/helm-charts/tree/main/charts/tempo
- **Promtail:** https://github.com/grafana/helm-charts/tree/main/charts/promtail
- **Opentelemetry:** https://github.com/open-telemetry/opentelemetry-operator
## Local installation steps
- Require a minikube node minimum 16G memory and 40G disk space and run on Ubuntu operator
```shell
minikube start --disk-size='40000mb' --memory='16g'
```
- Enable ingress addon
```shell
minikube addons enable ingress
```
- Install helm
  https://helm.sh/
- Install yq (the tool read, update yaml file)
  https://github.com/mikefarah/yq
- Goto `k8s-deployment` folder
- Execute [setup-keycloak.sh](setup-cluster.sh) to set up keycloak as the Identity and Access Management server.
```shell
./setup-keycloak.sh
```
- Execute [setup-redis.sh](setup-cluster.sh) to set up Redis as the server to store sessions for backends.
```shell
./setup-redis.sh
```
- Execute [setup-cluster.sh](setup-cluster.sh) to set up severs: `postgresql`, `elasticsearch`, `kafka`, `debezium connect`
```shell
./setup-cluster.sh
```
- Verify all servers run successful on namespaces: `postgres`, `elasticsearch`, `kafka`, `keycloak`
- After all above servers are running status, execute  [deploy-yas-applications.sh](deploy-yas-applications.sh) file to deploy all of yas applications to `yas` namespace
```shell
./deploy-yas-applications
```
All of YAS microservice deployed in `yas` namespace
- Setup hosts file
edit host file `/etc/hots`
```shell
192.168.49.2 pgoperator.yas.local.com
192.168.49.2 pgadmin.yas.local.com
192.168.49.2 akhq.yas.local.com
192.168.49.2 kibana.yas.local.com
192.168.49.2 identity.yas.local.com
192.168.49.2 backoffice.yas.local.com
192.168.49.2 storefront.yas.local.com
192.168.49.2 grafana.yas.local.com

```
`192.168.49.2` is ip of minikbe node use this command line to get the ip of minikube
```shell
minikube ip
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