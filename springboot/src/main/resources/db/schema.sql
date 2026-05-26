CREATE DATABASE IF NOT EXISTS user_management DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_management;

CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    age INT DEFAULT NULL,
    sex VARCHAR(10) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user_role_user_id (user_id),
    KEY idx_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permission (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(80) NOT NULL,
    type VARCHAR(20) NOT NULL,
    path VARCHAR(120) DEFAULT NULL,
    icon VARCHAR(50) DEFAULT NULL,
    sort INT DEFAULT 0,
    parent_id INT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permission (
    id INT NOT NULL AUTO_INCREMENT,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_permission_role_id (role_id),
    KEY idx_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) DEFAULT NULL,
    operation VARCHAR(100) DEFAULT NULL,
    method VARCHAR(20) DEFAULT NULL,
    url VARCHAR(255) DEFAULT NULL,
    params TEXT DEFAULT NULL,
    result TEXT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO users (username, password, nickname, age, sex, address)
SELECT 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 25, '男', '北京市朝阳区'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO role (name, code, description)
SELECT '管理员', 'ADMIN', '系统管理员'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'ADMIN');

INSERT INTO role (name, code, description)
SELECT '运营人员', 'OPERATOR', '负责用户维护和数据查看'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'OPERATOR');

INSERT INTO role (name, code, description)
SELECT '审计员', 'AUDITOR', '负责日志审计和风险分析'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'AUDITOR');

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u, role r
WHERE u.username = 'admin'
  AND r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '用户管理', 'menu:user', 'MENU', '/', 'User', 10, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'menu:user');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '仪表盘', 'menu:dashboard', 'MENU', '/dashboard', 'DataAnalysis', 20, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'menu:dashboard');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '角色管理', 'menu:role', 'MENU', '/roles', 'UserFilled', 30, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'menu:role');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '操作日志', 'menu:log', 'MENU', '/logs', 'Document', 40, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'menu:log');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '新增用户', 'btn:user:add', 'BUTTON', NULL, NULL, 101, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:user:add');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '编辑用户', 'btn:user:edit', 'BUTTON', NULL, NULL, 102, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:user:edit');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '删除用户', 'btn:user:delete', 'BUTTON', NULL, NULL, 103, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:user:delete');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '导入用户', 'btn:user:import', 'BUTTON', NULL, NULL, 104, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:user:import');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '导出用户', 'btn:user:export', 'BUTTON', NULL, NULL, 105, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:user:export');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT 'AI 数据解读', 'btn:ai:dashboard', 'BUTTON', NULL, NULL, 201, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:ai:dashboard');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT 'AI 日志分析', 'btn:ai:log', 'BUTTON', NULL, NULL, 202, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:ai:log');

INSERT INTO permission (name, code, type, path, icon, sort, parent_id)
SELECT '维护角色', 'btn:role:manage', 'BUTTON', NULL, NULL, 301, 0
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'btn:role:manage');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'OPERATOR'
  AND p.code IN ('menu:user', 'menu:dashboard', 'btn:user:add', 'btn:user:edit', 'btn:user:import', 'btn:user:export', 'btn:ai:dashboard')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'AUDITOR'
  AND p.code IN ('menu:dashboard', 'menu:log', 'btn:ai:dashboard', 'btn:ai:log')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
