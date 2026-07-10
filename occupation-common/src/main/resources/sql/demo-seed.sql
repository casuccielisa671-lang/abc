-- Safe UTF-8 seed via UNHEX (avoid shell encoding issues)
SET NAMES utf8mb4;

UPDATE sys_tenant
SET name = CONVERT(UNHEX('E6B58BE8AF95E5ADA6E99A2') USING utf8mb4),
    status = 1
WHERE id = 1;

SET @pwd = '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q';

UPDATE sys_user SET
  tenant_id = 1,
  password_hash = @pwd,
  real_name = CONVERT(UNHEX('E7B3BBE7BB9FE7AEA1E79086E59198') USING utf8mb4),
  status = 1
WHERE id = 1;

INSERT INTO sys_user (id, tenant_id, username, password_hash, role, real_name, status)
VALUES
  (2, 1, 'student', @pwd, 'STUDENT', CONVERT(UNHEX('E6BC94E7A4BAE5ADA6E7949F') USING utf8mb4), 1),
  (3, 1, 'teacher', @pwd, 'TEACHER', CONVERT(UNHEX('E6BC94E7A4BAE69599E5B888') USING utf8mb4), 1),
  (4, 1, 'hr',      @pwd, 'HR',      CONVERT(UNHEX('E6BC94E7A4BA4852') USING utf8mb4), 1)
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id),
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  real_name = VALUES(real_name),
  status = 1;
