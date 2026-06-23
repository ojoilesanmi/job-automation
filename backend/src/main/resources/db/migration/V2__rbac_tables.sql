-- V2: RBAC tables

-- Roles
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Permissions
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Role-Permission mapping
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- User-Role mapping (many-to-many)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed default roles
INSERT INTO roles (name, description) VALUES
    ('USER', 'Standard user with access to personal job applications'),
    ('ADMIN', 'Administrator with access to manage job sources and view all users'),
    ('SUPER_ADMIN', 'Super administrator with full system access');

-- Seed default permissions
INSERT INTO permissions (name, description) VALUES
    ('profile:read', 'Read own profile'),
    ('profile:write', 'Update own profile'),
    ('cv:read', 'Read own CVs'),
    ('cv:write', 'Upload and manage own CVs'),
    ('job:read', 'View jobs'),
    ('job:write', 'Import jobs manually'),
    ('job:discover', 'Trigger job discovery'),
    ('match:read', 'View job matches'),
    ('match:write', 'Approve or reject matches'),
    ('cover_letter:read', 'Read cover letters'),
    ('cover_letter:write', 'Generate and edit cover letters'),
    ('application:read', 'Read own applications'),
    ('application:write', 'Create and manage own applications'),
    ('application:submit', 'Submit applications'),
    ('preferences:read', 'Read own preferences'),
    ('preferences:write', 'Update own preferences'),
    ('admin:users:read', 'View all users'),
    ('admin:users:write', 'Manage user accounts'),
    ('admin:roles:read', 'View roles and permissions'),
    ('admin:roles:write', 'Manage roles and permissions'),
    ('admin:job_sources:read', 'View job sources'),
    ('admin:job_sources:write', 'Manage job sources'),
    ('admin:audit:read', 'View audit logs'),
    ('admin:dashboard:read', 'View system-wide dashboard');

-- Assign permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN (
    'profile:read', 'profile:write',
    'cv:read', 'cv:write',
    'job:read', 'job:write',
    'match:read', 'match:write',
    'cover_letter:read', 'cover_letter:write',
    'application:read', 'application:write', 'application:submit',
    'preferences:read', 'preferences:write'
);

-- Assign permissions to ADMIN role (everything USER has + admin permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN';

-- Assign permissions to SUPER_ADMIN role (everything)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN';
