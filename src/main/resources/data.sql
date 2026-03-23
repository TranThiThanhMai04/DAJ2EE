SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Tạo các vai trò trước
INSERT IGNORE INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT IGNORE INTO roles (id, name) VALUES (2, 'ROLE_TENANT');

-- Tạo các quyền hạn (Permissions)
INSERT IGNORE INTO permissions (id, name) VALUES (1, 'OP_EDIT_ROOM');
INSERT IGNORE INTO permissions (id, name) VALUES (2, 'OP_DELETE_ROOM');
INSERT IGNORE INTO permissions (id, name) VALUES (3, 'OP_VIEW_REPORT');
INSERT IGNORE INTO permissions (id, name) VALUES (4, 'OP_EDIT_INVOICE');

-- Gán quyền cho Vai trò (Role-Permission mapping)
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (1, 1);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (1, 2);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (1, 3);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (1, 4);
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (2, 3);

-- Tạo tài khoản Admin
INSERT INTO users (id, username, password, full_name, email, role_id, status) 
VALUES (1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqCYAd1up5fLndN7YFvX.A5hVn6G', 'Phạm Thị Ái Ngân', 'ngan.admin@gmail.com', 1, 1)
ON DUPLICATE KEY UPDATE 
    full_name = VALUES(full_name),
    email = VALUES(email);

