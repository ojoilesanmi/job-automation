-- Seed: Promote first user to SUPER_ADMIN
-- Usage: psql -h localhost -U jobagent -d jobagent -f seed_admin.sql

-- Find the first registered user and assign SUPER_ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE r.name = 'SUPER_ADMIN'
  AND u.id NOT IN (SELECT ur.user_id FROM user_roles ur JOIN roles ro ON ur.role_id = ro.id WHERE ro.name = 'SUPER_ADMIN')
ORDER BY u.created_at ASC
LIMIT 1
ON CONFLICT DO NOTHING;

-- Verify
SELECT u.email, r.name AS role
FROM user_roles ur
JOIN users u ON ur.user_id = u.id
JOIN roles r ON ur.role_id = r.id
WHERE r.name = 'SUPER_ADMIN';
