# Hướng Dẫn Jenkinsfile (Microservices CI)

Tài liệu này hướng dẫn chi tiết cách Jenkinsfile hiện tại hoạt động và cách mở rộng an toàn khi cần:

- Thêm service mới vào pipeline
- Thêm stage command mới cho service
- Hiểu điều kiện service nào được chạy khi có thay đổi code

## 1. Tổng quan luồng chạy

Jenkinsfile hiện tại sử dụng một danh sách cấu hình trung tâm:

- Biến `microservices`: danh sách service được theo dõi và command của từng service.
- Hàm `collectChangedPaths()`: lấy danh sách file thay đổi từ SCM changelog.
- Hàm `runServicePipeline(service)`: tạo stage và chạy command theo từng mục trong `commands`.
- Khối `parallel`: chạy các service song song (`failFast: false`, một service lỗi không dừng ngay service khác).

Pipeline sẽ quyết định có chạy một service hay không dựa trên:

1. Không có SCM context (`changedPaths` rỗng): chạy tất cả service.
2. File gốc `pom.xml` bị thay đổi: chạy tất cả service.
3. Có file thay đổi trong thư mục service, ví dụ `payment/...`: chỉ chạy service đó.

Nếu service được chọn nhưng `commands` rỗng, Jenkins sẽ log:

- `No commands defined for <Service Display>, skipping...`

## 2. Cấu trúc cấu hình service

Mỗi service trong `microservices` có cấu trúc:

~~~groovy
[
	id: 'service-name',
	display: 'Service Display Name',
	commands: [
		[name: 'Stage Name', command: 'your shell command'],
		[name: 'Unit Test', command: 'mvn test -pl service-name']
	]
]
~~~

Ý nghĩa:

- `id`: tên thư mục service trong repo (bắt buộc phải đúng).
- `display`: tên hiển thị trên giao diện Jenkins.
- `commands`: danh sách stage cho service đó.
- `commands[].name`: tên stage hiển thị.
- `commands[].command`: lệnh shell thực thi bằng `sh`.

## 3. Cách thêm service mới

### Bước 1: Tạo thư mục service

Đảm bảo service mới có thư mục đúng tên `id` ở root repo.

Ví dụ: muốn thêm service `coupon` thì cần có folder `coupon/`.

### Bước 2: Thêm entry vào `microservices` trong [Jenkinsfile](Jenkinsfile)

Chèn thêm 1 phần tử mới vào danh sách `microservices`:

~~~groovy
[id: 'coupon', display: 'Coupon Service', commands: [
	[name: 'Unit Test', command: 'mvn -pl coupon -am test'],
	[name: 'Build', command: 'mvn -pl coupon -am package -DskipTests']
]]
~~~

Lưu ý quan trọng:

1. `id` phải trùng khớp 100% với tên thư mục service.
2. Không đặt trùng `display` để tránh đè stage trong Jenkins UI.
3. Nếu để `commands: []` thì service được detect thay đổi nhưng sẽ bị skip khi chạy.

### Bước 3: Tạo commit thay đổi trong service mới

Để test logic filter, thay đổi 1 file bên trong `coupon/` rồi push.

Kỳ vọng:

- Jenkins log `Running pipeline for Coupon Service`.
- Các stage trong `commands` của `coupon` sẽ được tạo và chạy.

## 4. Cách thêm stage command mới cho service đã có

Mở service cần sửa trong `microservices`, thêm phần tử vào `commands`.

Ví dụ thêm stage `Static Analysis` cho `customer`:

~~~groovy
[id: 'customer', display: 'Customer Service', commands: [
	[name: 'Unit Test', command: 'echo "Running unit tests for Customer Service"'],
	[name: 'Integration Test', command: 'echo "Running integration tests for Customer Service"'],
	[name: 'Static Analysis', command: 'mvn -pl customer -am verify -DskipTests'],
	[name: 'Build', command: 'echo "Building Customer Service"']
]]
~~~

Lưu ý:

1. Thứ tự trong `commands` là thứ tự stage hiển thị/chạy.
2. `name` nên ngắn gọn, rõ nghĩa.
3. `command` là shell command thực thi trong workspace root.
4. Nếu command cần vào thư mục service, dùng dạng:

~~~groovy
[name: 'Lint', command: 'cd customer && mvn checkstyle:check']
~~~

## 5. Quy tắc trigger theo file thay đổi

Jenkinsfile đang dùng quy tắc đơn giản và hiệu quả:

- Thay đổi `pom.xml` gốc -> tất cả service chạy.
- Thay đổi file trong `serviceId/...` -> service đó chạy.
- Không có changelog (manual build/rebuild) -> tất cả service chạy.

Ví dụ:

1. Sửa `product/src/...` -> chỉ Product Service được chạy.
2. Sửa `pom.xml` -> toàn bộ service trong `microservices` được xét để chạy.
3. Manual build không có SCM changes -> toàn bộ service được xét để chạy.

## 6. Mẫu cấu hình để copy nhanh

~~~groovy
[id: 'your-service-id', display: 'Your Service Name', commands: [
	[name: 'Unit Test', command: 'mvn -pl your-service-id -am test'],
	[name: 'Build', command: 'mvn -pl your-service-id -am package -DskipTests'],
	[name: 'Docker Build', command: 'echo "Build docker image for your-service-id"']
]]
~~~

## 7. Gợi ý chuẩn hóa command cho dự án Maven multi-module

Nên ưu tiên command có `-pl <module> -am` để:

- Build/test đúng phạm vi service.
- Vẫn build được dependencies cần thiết.
- Giảm thời gian so với build all modules.

Ví dụ:

- Test: `mvn -pl payment -am test`
- Package: `mvn -pl payment -am package -DskipTests`

## 8. Checklist trước khi merge thay đổi Jenkinsfile

1. Đã thêm/sửa đúng service trong `microservices`.
2. `id` trùng tên thư mục service.
3. Các command chạy được trên Jenkins agent.
4. Stage name rõ ràng, không trùng lặp khó hiểu.
5. Đã test bằng 1 commit thay đổi vào đúng thư mục service.

---