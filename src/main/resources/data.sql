-- Tạo các vai trò trước
INSERT IGNORE INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT IGNORE INTO roles (id, name) VALUES (2, 'ROLE_TENANT');

-- Tạo tài khoản Admin Ngân
-- Mật khẩu bên dưới là '123456' đã được mã hóa BCrypt (Bắt buộc phải mã hóa)
INSERT IGNORE INTO users (id, username, password, full_name, email, role_id, status) 
VALUES (1, 'admin', '$2a$10$dwjmMuDVGuQXcQDBIkf1I.z2ctBCri8CcTIvbxs88YZJigblqyHVm', 'Phạm Thị Ái Ngân', 'ngan.admin@gmail.com', 1, 1);