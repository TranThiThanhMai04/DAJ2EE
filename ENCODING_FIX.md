Khi bị lỗi hiển thị chữ (ký tự lạ)

1) Những việc mình đã làm trong project:
- Cập nhật `src/main/resources/application.properties` để JDBC kết nối dùng `utf8mb4` và sửa tên database thành `apartmentmanagement` (chạy cùng tên với phpMyAdmin).
- Thêm file `sql/convert_to_utf8mb4.sql` chứa các lệnh`ALTER DATABASE` / `ALTER TABLE` để chuyển schema và bảng sang `utf8mb4`.

2) Cách chạy (hai cách):
- Dùng MySQL client (terminal/PowerShell):
  ```bash
  mysql -u root -p < sql/convert_to_utf8mb4.sql
  ```
- Hoặc mở phpMyAdmin, chọn database `apartmentmanagement`, mở tab SQL và dán toàn bộ nội dung `sql/convert_to_utf8mb4.sql`, rồi chạy.

3) Sau khi chạy:
- Refresh phpMyAdmin, clear cache trình duyệt và reload trang ứng dụng.
- Nếu vẫn thấy ký tự lạ, trong MySQL client chạy `SET NAMES 'utf8mb4'; SELECT * FROM services LIMIT 1;` để kiểm tra dữ liệu thô.

4) Ghi chú an toàn:
- Trước khi chạy các lệnh thay đổi charset, nên sao lưu database (export) phòng lỗi.
