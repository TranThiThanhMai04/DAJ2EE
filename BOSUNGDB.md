# 🚨 GHI CHÚ BỔ SUNG DATABASE TRÊN NHÁNH DEVELOP 🚨

Chào team, tôi (Ngân) vừa hoàn thiện luồng **Hệ thống Đăng ký chặn chờ Admin duyệt (Pending Approvals)**. Để chạy được code mới nhất, mọi người vui lòng chú ý cập nhật Database của từng cá nhân theo hướng dẫn sau nhé!

---

## 1. Nội dung thay đổi ở file gốc `database.db` 
*(Dành cho các bạn thiết lập lại Database từ đầu hoặc sửa script chung)*

Tôi đã bổ sung thêm cột `enabled BOOLEAN DEFAULT FALSE` vào bảng `users`. Mẫu bảng chuẩn hiện tại phải như sau:

```sql
-- 2. Bảng Người dùng
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255), 
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15),
    cccd VARCHAR(15) UNIQUE,
    avatar_url VARCHAR(255),
    role_id BIGINT,
    provider VARCHAR(20) DEFAULT 'LOCAL', 
    status BOOLEAN DEFAULT TRUE,
    
    enabled BOOLEAN DEFAULT FALSE, -- [UPDATE MỚI] Dùng để kiểm duyệt Cư dân có được Đăng nhập không
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

---

## 2. Cách cập nhật trên máy cá nhân của team (Quan Trọng)

**📌 Tình huống A: Ai lười tạo lại DB và ĐANG CÓ SẴN Database trên máy**
Bạn không cần xóa Database cũ. Chỉ cần mở phần mềm DBeaver, MySQL Workbench hoặc XAMPP và **chạy một câu lệnh duy nhất** sau:

```sql
ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;
```
*(Giải thích: Lệnh này tự tạo thêm cột `enabled`. Đặt `DEFAULT 1` để các dữ liệu test/tài khoản mọi người đang dùng bình thường không bị khoá oan ngớ ngẩn).*


**📌 Tình huống B: Ai chọn cách DROP DATABASE để làm lại từ đầu**
Chỉ cần copy cấu trúc Bảng ở Mục 1 đè vào file `database.db` rồi chạy cài đặt bình thường. Khởi động Server Spring Boot sẽ tự nạp dữ liệu Admin mà không gặp lỗi gì cả!

---

## 3. Nội dung thay đổi ở file `data.sql`
*(Đây là file tự động tạo Admin ban đầu khi chạy Spring Boot)*

Vì bảng `users` đã có thêm cột `enabled`, nên tôi (Ngân) đã cập nhật lệnh `INSERT` tài khoản Admin để cấp sẵn ID=1 này trạng thái `enabled = true` (Đã duyệt - Có quyền đăng nhập ngay).

**Đoạn code đã sửa (dòng 23-31 trong `data.sql`):**
```sql
INSERT INTO users (id, username, password, full_name, email, role_id, status, enabled) 
VALUES (1, 'admin', '$2a$10$8...mã_hóa...', 'Phạm Thị Ái Ngân', 'ngan.admin@gmail.com', 1, 1, true)
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    full_name = VALUES(full_name),
    email = VALUES(email),
    role_id = VALUES(role_id),
    status = VALUES(status),
    enabled = VALUES(enabled);
```
