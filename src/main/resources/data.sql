INSERT IGNORE INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT IGNORE INTO roles (id, name) VALUES (2, 'ROLE_TENANT');

INSERT INTO users (id, username, password, full_name, email, role_id, status) 
VALUES (1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqCYAd1up5fLndN7YFvX.A5hVn6G', 'Phạm Thị Ái Ngân', 'ngan.admin@gmail.com', 1, 1)
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    full_name = VALUES(full_name),
    email = VALUES(email),
    role_id = VALUES(role_id),
    status = VALUES(status);