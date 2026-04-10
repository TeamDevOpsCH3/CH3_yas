# T05 - Monorepo Analysis (CH3_yas)

## 1. Mục tiêu

Tài liệu này phân tích chi tiết monorepo `CH3_yas` để phục vụ task T05, tập trung vào:

1. Cấu trúc mã nguồn và phân loại module.
2. Đồ thị build (Maven/Node), chiến lược test và CI/CD.
3. Runtime topology trên Docker Compose.
4. Bản đồ phụ thuộc service (HTTP + data + event).
5. Rủi ro kỹ thuật và đề xuất cải tiến theo mức ưu tiên.

Thời điểm snapshot: 2026-04-10.

---

## 2. Phạm vi và nguồn dữ liệu

Phân tích được tổng hợp từ các nguồn trong repo:

- Root Maven aggregator: `pom.xml`.
- Runtime orchestration:
  - `docker-compose.yml`
  - `docker-compose.search.yml`
  - `docker-compose.o11y.yml`
  - `.env`
- Scripts vận hành:
  - `start-yas.sh`
  - `start-source-connectors.sh`
  - `workflows.sh`
- Tài liệu kỹ thuật:
  - `README.md`
  - `docs/README.md`
  - `docs/developer-guidelines.md`
- Workflow inventory: `.github/workflows/*.yml`.

---

## 3. Tổng quan monorepo

### 3.1 Loại hình monorepo

Đây là monorepo polyglot kết hợp:

- Backend microservices bằng Java + Spring Boot.
- Frontend Next.js (2 app: storefront, backoffice).
- BFF layer (Spring Cloud Gateway): `storefront-bff`, `backoffice-bff`.
- Automation UI test module (Maven-based), tách khỏi root reactor.
- DevOps assets: Docker Compose, Kubernetes manifests/charts, observability stack, CDC connectors.

### 3.2 Nhóm thư mục chính theo domain

1. **Business services (backend):** `cart`, `customer`, `inventory`, `location`, `media`, `order`, `payment`, `payment-paypal`, `product`, `promotion`, `rating`, `search`, `tax`, `webhook`, `recommendation`, `delivery`, `sampledata`.
2. **Gateway/BFF:** `storefront-bff`, `backoffice-bff`.
3. **Frontend:** `storefront`, `backoffice`.
4. **Shared code:** `common-library`.
5. **Automation test UI:** `automation-ui/*`.
6. **Infra/ops:** `docker`, `deployment`, `k8s`, `kafka`, `nginx`, `identity`, và các script ở root.

---

## 4. Build graph và module inventory

### 4.1 Root Maven reactor (thứ tự build chính thức)

Root `pom.xml` khai báo 20 module theo thứ tự sau:

1. `common-library`
2. `backoffice-bff`
3. `cart`
4. `customer`
5. `inventory`
6. `location`
7. `media`
8. `order`
9. `payment-paypal`
10. `payment`
11. `product`
12. `promotion`
13. `rating`
14. `search`
15. `storefront-bff`
16. `tax`
17. `webhook`
18. `sampledata`
19. `recommendation`
20. `delivery`

### 4.1.1 Inventory Maven modules (thực tế trong workspace)

| Module                          | Dockerfile | src/main/java | src/test/java | src/it/java | README module |
| ------------------------------- | ---------: | ------------: | ------------: | ----------: | ------------: |
| root (`CH3_yas`)                |         No |            No |            No |          No |           Yes |
| `automation-ui`                 |         No |            No |            No |          No |           Yes |
| `automation-ui/automation-base` |         No |           Yes |            No |          No |            No |
| `automation-ui/backoffice`      |         No |           Yes |           Yes |          No |            No |
| `automation-ui/storefront`      |         No |           Yes |           Yes |          No |            No |
| `backoffice-bff`                |        Yes |           Yes |            No |          No |            No |
| `cart`                          |        Yes |           Yes |           Yes |         Yes |            No |
| `common-library`                |         No |           Yes |           Yes |         Yes |            No |
| `customer`                      |        Yes |           Yes |           Yes |         Yes |            No |
| `delivery`                      |         No |           Yes |            No |          No |            No |
| `inventory`                     |        Yes |           Yes |           Yes |         Yes |            No |
| `location`                      |        Yes |           Yes |           Yes |         Yes |            No |
| `media`                         |        Yes |           Yes |           Yes |         Yes |            No |
| `order`                         |        Yes |           Yes |           Yes |         Yes |            No |
| `payment`                       |        Yes |           Yes |           Yes |         Yes |            No |
| `payment-paypal`                |        Yes |           Yes |           Yes |         Yes |            No |
| `product`                       |        Yes |           Yes |           Yes |         Yes |            No |
| `promotion`                     |        Yes |           Yes |           Yes |         Yes |            No |
| `rating`                        |        Yes |           Yes |           Yes |         Yes |            No |
| `recommendation`                |        Yes |           Yes |           Yes |         Yes |            No |
| `sampledata`                    |        Yes |           Yes |            No |          No |            No |
| `search`                        |        Yes |           Yes |           Yes |         Yes |            No |
| `storefront-bff`                |        Yes |           Yes |            No |          No |            No |
| `tax`                           |        Yes |           Yes |           Yes |         Yes |            No |
| `webhook`                       |        Yes |           Yes |           Yes |         Yes |            No |

Lưu ý: Bảng inventory tập trung vào cấu trúc source/test để phục vụ CI pipeline, không phụ thuộc trạng thái artifact local.

### 4.2 Node modules

| Module       | Dockerfile | Có src/pages/app | Có public | README |
| ------------ | ---------: | ---------------: | --------: | -----: |
| `backoffice` |        Yes |              Yes |       Yes |    Yes |
| `storefront` |        Yes |              Yes |       Yes |    Yes |

### 4.3 Build conventions quan trọng

1. Root Maven sử dụng `packaging=pom` và plugin-management trung tâm.
2. Unit test và integration test được tách:
   - `src/test/java`
   - `src/it/java`
3. Failsafe được cấu hình để chạy test `**/*IT.java`.
4. Jacoco, Checkstyle và OWASP dependency-check được khai báo ở root level.
5. `workflows.sh` có script sinh workflow từ template, nhưng danh sách service trong script hiện chưa khớp hoàn toàn với danh sách module hiện tại (dấu hiệu drift).

---

## 5. Runtime topology (Docker Compose)

### 5.1 Core stack (`docker-compose.yml`)

Danh sách service key:

- `nginx`
- `identity`
- `backoffice`
- `backoffice-nextjs`
- `storefront`
- `storefront-nextjs`
- `media`
- `product`
- `customer`
- `cart`
- `rating`
- `order`
- `payment`
- `location`
- `inventory`
- `promotion`
- `tax`
- `sampledata`
- `swagger-ui`
- `postgres`
- `pgadmin`
- `zookeeper`
- `kafka`
- `kafka-connect`
- `kafka-ui`
- `redis`

Lưu ý runtime:

- `webhook` và `recommendation` đang có block cấu hình bị comment trong compose core (không chạy mặc định).
- `storefront`/`backoffice` trong compose là BFF service (Java), còn `*-nextjs` là UI container.

### 5.2 Search stack (`docker-compose.search.yml`)

- `search`
- `elasticsearch`

### 5.3 Observability stack (`docker-compose.o11y.yml`)

- `collector` (OTel collector)
- `prometheus`
- `grafana`
- `loki`
- `tempo`

### 5.4 Compose profile mặc định

`.env` đang set:

- `COMPOSE_FILE=docker-compose.yml:docker-compose.search.yml:docker-compose.o11y.yml`

=> `docker compose up` sẽ kéo toàn bộ core + search + observability.

---

## 6. Dependency map (service-to-service)

Suy luận từ biến môi trường `YAS_SERVICES_*` trong compose/.env:

### 6.1 BFF layer

- `storefront-bff` gọi: `customer`, `cart`, `rating`, `order`, `location`, `inventory`, `tax`, `promotion`, `payment`, `sampledata`.
- `backoffice-bff` đóng vai trò API gateway/proxy cho backoffice flow.

### 6.2 Backend call graph chính

- `product` -> `media`
- `cart` -> `media`, `product`
- `customer` -> `location`
- `rating` -> `product`, `customer`, `order`
- `order` -> `cart`, `customer`, `product`, `tax`
- `payment` -> `order`, `media`
- `inventory` -> `product`, `location`
- `promotion` -> `product`
- `tax` -> `location`
- `search` -> `product`

### 6.3 Data storage

Đa số service sử dụng PostgreSQL schema/database riêng theo tên service (ví dụ: `product`, `customer`, `order`, `tax`...).

### 6.4 Event-driven/CDC

- Kafka stack gồm `zookeeper`, `kafka`, `kafka-connect`.
- Script `start-source-connectors.sh` đang push 2 connector:
  - `product-connector`
  - `order-connector`
- Search service cần được cập nhật dữ liệu từ event pipeline + Elasticsearch.

---

## 7. CI/CD hiện trạng trong monorepo

### 7.1 Workflow inventory

Hiện có 23 workflow files trong `.github/workflows`, theo mô hình tách theo service:

- Service workflows: `backoffice-bff-ci.yaml`, `backoffice-ci.yaml`, `cart-ci.yaml`, `customer-ci.yaml`, `inventory-ci.yaml`, `location-ci.yaml`, `media-ci.yaml`, `order-ci.yaml`, `payment-ci.yaml`, `payment-paypal-ci.yaml`, `product-ci.yaml`, `promotion-ci.yaml`, `rating-ci.yaml`, `recommendation-ci.yaml`, `sampledata-ci.yaml`, `search-ci.yaml`, `storefront-bff-ci.yaml`, `storefront-ci.yaml`, `tax-ci.yaml`, `webhook-ci.yaml`.
- Security/quality workflows: `gitleaks-check.yaml`, `codeql.yml`.
- Infra workflow: `charts-ci.yaml`.

### 7.2 Đặc điểm CI trong monorepo

1. Trigger theo `paths` để tránh build toàn bộ module khi thay đổi cục bộ.
2. Mỗi service có pipeline build/test/scan/image riêng.
3. SonarCloud + test report + image push đã được tích hợp (theo docs).

### 7.3 Khoảng trống so với Jenkins migration

- Branch hiện tại không có `Jenkinsfile` trong workspace (scan `**/Jenkinsfile` trả về rỗng).
- Để migrate sang Jenkins, cần map lại 23 workflow sang pipeline strategy (multibranch hoặc matrix/templated pipeline).

**Quyết định của nhóm:** Chọn Option A (1 Jenkinsfile root + path-based stages), vì phù hợp với hướng triển khai T07/T08.

---

## 8. Đánh giá điểm mạnh và rủi ro

### 8.1 Điểm mạnh

1. **Single source of truth:** tất cả service trong một repo, dễ đồng bộ issue và architecture.
2. **Path-based CI:** tối ưu thời gian build cho monorepo lớn.
3. **Runtime stack đầy đủ:** local env có auth, data, queue, search, observability.
4. **Test architecture rõ ràng:** tách unit/integration test theo convention.
5. **Container-first:** phần lớn service có Dockerfile riêng.

### 8.2 Rủi ro / tech debt

1. **Drift giữa build graph và module thực tế:**
   - `automation-ui/*` có Maven modules nhưng không nằm trong root reactor.
   - Tác động CI: nếu thay đổi chỉ xảy ra trong `automation-ui/*`, pipeline root theo module list hiện tại có thể không trigger hoặc không build/test đầy đủ phần automation.
2. **Runtime drift:**
   - `webhook` và `recommendation` tồn tại module nhưng đang bị comment trong compose core.
3. **Workflow duplication cost:**
   - 23 workflow files cần maintenance.
4. **Script drift:**
   - `workflows.sh` đang tham chiếu service list chưa đồng bộ với inventory hiện tại.
5. **Memory pressure local:**
   - full stack cần tài nguyên cao (docs cảnh báo >=16GB RAM).
6. **Chưa có Jenkins pipeline contract hiện hành trong branch:**
   - cần xác định migration strategy rõ trước T09/T10.

---

## 9. Đề xuất hành động (ưu tiên cho team)

### P0 - Ngay lập tức (để chốt T05)

1. Chốt **monorepo inventory baseline** (tài liệu này).
2. Chốt **module ownership matrix** (service -> owner team member).
3. Áp dụng quyết định Jenkins cho T07/T08: **Option A (1 Jenkinsfile root + path-based stages)**.

### P1 - Ngắn hạn (sau T05)

1. Đồng bộ `workflows.sh` với inventory module hiện tại.
2. Quy định rõ module nào là optional runtime (`webhook`, `recommendation`) và profile bật/tắt.
3. Chuẩn hóa README module-level (hiện module README còn ít).

### P2 - Trung hạn

1. Refactor CI templates để giảm duplication.
2. Thêm quality gate thống nhất cho Node modules (lint/test policy).
3. Tạo architecture diagram service dependency từ compose + env (có thể auto-generate).

---

## 10. Inputs hữu ích cho task T06 (Jenkins credentials)

Từ inventory hiện tại, cần dự kiến các nhóm credentials sau:

1. **Container registry** (GHCR): username/token push image.
2. **Code quality**: Sonar token.
3. **Security scan**: nếu dùng external scanner cần token riêng.
4. **Optional AI service** (`recommendation`): Azure OpenAI key/endpoint (đang để trống trong `.env`).
5. **GitHub webhook/personal token** cho trigger + checkout private scopes (nếu cần).

Mục này giúp liên kết trực tiếp T05 -> T06 thay vì phân tích rời rạc.

---

## 11. Tóm tắt điều hành

Monorepo `CH3_yas` đã có nền tảng kỹ thuật khá đầy đủ cho microservices (build graph rõ, compose stack phong phú, path-based CI hợp lý). Tuy nhiên, đã xuất hiện các dấu hiệu drift giữa codebase, runtime profile và automation scripts. Task T05 nên được đóng bằng baseline inventory + dependency map + roadmap cleanup nhẹ để tạo tiền đề chắc chắn cho T06 (credential management) và T09/T10 (Jenkins pipeline migration).

---

## 12. Coverage baseline (phục vụ T09-T11)

| Service          | Has src/test/java | Estimated test count | Build local OK?               |
| ---------------- | :---------------: | -------------------: | ----------------------------- |
| `common-library` |        Yes        |             ~4 files | Yes (2026-04-10, -DskipTests) |
| `backoffice-bff` |        No         |             ~0 files | Yes (2026-04-10, -DskipTests) |
| `cart`           |        Yes        |             ~4 files | Yes (2026-04-10, -DskipTests) |
| `customer`       |        Yes        |             ~6 files | Yes (2026-04-10, -DskipTests) |
| `inventory`      |        Yes        |             ~6 files | Yes (2026-04-10, -DskipTests) |
| `location`       |        Yes        |             ~7 files | Yes (2026-04-10, -DskipTests) |
| `media`          |        Yes        |             ~2 files | Yes (2026-04-10, -DskipTests) |
| `order`          |        Yes        |            ~11 files | Yes (2026-04-10, -DskipTests) |
| `payment-paypal` |        Yes        |             ~4 files | Yes (2026-04-10, -DskipTests) |
| `payment`        |        Yes        |             ~5 files | Yes (2026-04-10, -DskipTests) |
| `product`        |        Yes        |            ~16 files | Yes (2026-04-10, -DskipTests) |
| `promotion`      |        Yes        |             ~5 files | Yes (2026-04-10, -DskipTests) |
| `rating`         |        Yes        |             ~5 files | Yes (2026-04-10, -DskipTests) |
| `search`         |        Yes        |             ~4 files | Yes (2026-04-10, -DskipTests) |
| `storefront-bff` |        No         |             ~0 files | Yes (2026-04-10, -DskipTests) |
| `tax`            |        Yes        |             ~1 files | Yes (2026-04-10, -DskipTests) |
| `webhook`        |        Yes        |             ~4 files | Yes (2026-04-10, -DskipTests) |
| `sampledata`     |        No         |             ~0 files | Yes (2026-04-10, -DskipTests) |
| `recommendation` |        Yes        |             ~5 files | Yes (2026-04-10, -DskipTests) |
| `delivery`       |        No         |             ~0 files | Yes (2026-04-10, -DskipTests) |

Ghi chú:

- `Estimated test count` được tính theo số file `.java` trong `src/test/java` tại thời điểm phân tích.
- Cột `Build local OK?` đã được xác minh bằng local build root reactor: `.\mvnw.cmd -DskipTests clean package` (BUILD SUCCESS).
- Module `automation-ui` được build riêng bằng: `.\automation-ui\mvnw.cmd -DskipTests clean package` (BUILD SUCCESS).
