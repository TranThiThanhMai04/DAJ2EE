
CREATE DATABASE ApartmentManagement CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ApartmentManagement;


ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;

SELECT * FROM users;
ALTER TABLE users 
ADD COLUMN gender ENUM('MALE', 'FEMALE', 'OTHER') AFTER full_name,
ADD COLUMN hometown VARCHAR(100) AFTER cccd;
-- 1. Bảng Vai trò
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) UNIQUE NOT NULL -- ROLE_ADMIN, ROLE_TENANT
);

-- 2. Bảng Người dùng
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255), -- Để trống nếu login Google/OAuth2
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15),
    cccd VARCHAR(15) UNIQUE,
    avatar_url VARCHAR(255),
    role_id BIGINT,
    provider VARCHAR(20) DEFAULT 'LOCAL', -- LOCAL hoặc GOOGLE
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ========================================================
-- PHẦN CỦA MẠNH: QUẢN LÝ PHÒNG TRỌ
-- ========================================================

-- 3. Bảng Phòng trọ
CREATE TABLE rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    area FLOAT,
    description TEXT,
    status ENUM('EMPTY', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'EMPTY'
);

-- ========================================================
-- PHẦN CỦA MINH: QUẢN LÝ HỢP ĐỒNG
-- ========================================================

-- 4. Bảng Hợp đồng
CREATE TABLE contracts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT, -- Người thuê
    room_id BIGINT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    deposit DECIMAL(15, 2), -- Tiền cọc
    status ENUM('ACTIVE', 'EXPIRED', 'TERMINATED') DEFAULT 'ACTIVE',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL
);

-- ========================================================
-- PHẦN CỦA MAI & LIỄU: DỊCH VỤ & CHỈ SỐ
-- ========================================================

-- 5. Bảng Danh mục Dịch vụ (Điện, Nước, Internet...)
CREATE TABLE services (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_name VARCHAR(50) NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    unit VARCHAR(20) -- kWh, m3, tháng
);

-- 6. Bảng Chỉ số sử dụng hàng tháng (Phần của Mai)
CREATE TABLE service_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT,
    service_id BIGINT,
    old_value INT NOT NULL,
    new_value INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(id)
);

-- ========================================================
-- PHẦN CỦA LIỄU & HẰNG: HÓA ĐƠN & CÔNG NỢ
-- ========================================================

-- 7. Bảng Hóa đơn tổng
CREATE TABLE invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contract_id BIGINT,
    month INT NOT NULL,
    year INT NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    payment_status ENUM('UNPAID', 'PAID', 'OVERDUE') DEFAULT 'UNPAID',
    payment_date DATETIME,
    due_date DATE,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE
);

-- ========================================================
-- PHẦN CỦA QUÂN: YÊU CẦU SỬA CHỮA
-- ========================================================

-- 8. Bảng Feedback / Bảo trì
CREATE TABLE maintenance_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    room_id BIGINT,
    description TEXT NOT NULL,
    image_url VARCHAR(255),
    status ENUM('PENDING', 'PROCESSING', 'DONE') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

-- ========================================================
-- PHẦN CỦA LIÊU: THÔNG BÁO & BÁO CÁO
-- ========================================================

-- 9. Bảng Thông báo chung
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


