-- V1: Initial schema for Job Application Agent Platform

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- User profiles
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    headline VARCHAR(255),
    summary TEXT,
    location VARCHAR(255),
    years_of_experience INTEGER,
    primary_role VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id)
);

-- CV documents
CREATE TABLE cv_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    parsed_text TEXT,
    version_name VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_cv_documents_user_id ON cv_documents(user_id);

-- Profile skills
CREATE TABLE profile_skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_name VARCHAR(255) NOT NULL,
    skill_type VARCHAR(50) NOT NULL DEFAULT 'technical',
    proficiency VARCHAR(50),
    years_used INTEGER
);

CREATE INDEX idx_profile_skills_user_id ON profile_skills(user_id);

-- Work experiences
CREATE TABLE work_experiences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    description TEXT,
    achievements TEXT
);

CREATE INDEX idx_work_experiences_user_id ON work_experiences(user_id);

-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    technologies TEXT,
    url VARCHAR(512)
);

CREATE INDEX idx_projects_user_id ON projects(user_id);

-- Job sources
CREATE TABLE job_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    base_url VARCHAR(512),
    enabled BOOLEAN DEFAULT TRUE,
    config_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Jobs
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    external_job_id VARCHAR(255),
    source_id UUID REFERENCES job_sources(id),
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    company_website VARCHAR(512),
    description TEXT NOT NULL,
    location VARCHAR(255),
    country VARCHAR(100),
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    currency VARCHAR(10),
    remote_type VARCHAR(50),
    relocation_available BOOLEAN DEFAULT FALSE,
    visa_sponsorship_signal BOOLEAN DEFAULT FALSE,
    seniority VARCHAR(50),
    required_skills TEXT,
    preferred_skills TEXT,
    experience_years INTEGER,
    employment_type VARCHAR(50),
    application_url VARCHAR(512),
    ats_provider VARCHAR(100),
    date_posted TIMESTAMP WITH TIME ZONE,
    date_discovered TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    raw_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(source_id, external_job_id)
);

CREATE INDEX idx_jobs_source_id ON jobs(source_id);
CREATE INDEX idx_jobs_company ON jobs(company);
CREATE INDEX idx_jobs_country ON jobs(country);
CREATE INDEX idx_jobs_remote_type ON jobs(remote_type);
CREATE INDEX idx_jobs_date_discovered ON jobs(date_discovered);

-- Job matches
CREATE TABLE job_matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    fit_score DECIMAL(5,2) NOT NULL,
    skills_score DECIMAL(5,2),
    experience_score DECIMAL(5,2),
    role_score DECIMAL(5,2),
    location_score DECIMAL(5,2),
    salary_score DECIMAL(5,2),
    matched_skills TEXT,
    missing_skills TEXT,
    reasons_to_apply TEXT,
    reasons_to_skip TEXT,
    risk_flags TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, job_id)
);

CREATE INDEX idx_job_matches_user_id ON job_matches(user_id);
CREATE INDEX idx_job_matches_job_id ON job_matches(job_id);
CREATE INDEX idx_job_matches_status ON job_matches(status);
CREATE INDEX idx_job_matches_fit_score ON job_matches(fit_score DESC);

-- Cover letters
CREATE TABLE cover_letters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    cv_document_id UUID REFERENCES cv_documents(id),
    content TEXT NOT NULL,
    version INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'draft',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_cover_letters_user_id ON cover_letters(user_id);
CREATE INDEX idx_cover_letters_job_id ON cover_letters(job_id);

-- Applications
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    cv_document_id UUID REFERENCES cv_documents(id),
    cover_letter_id UUID REFERENCES cover_letters(id),
    status VARCHAR(50) NOT NULL DEFAULT 'discovered',
    application_mode VARCHAR(50) DEFAULT 'approval',
    submitted_at TIMESTAMP WITH TIME ZONE,
    source_url VARCHAR(512),
    notes TEXT,
    last_follow_up_at TIMESTAMP WITH TIME ZONE,
    next_follow_up_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, job_id)
);

CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_submitted_at ON applications(submitted_at);

-- Application events
CREATE TABLE application_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_application_events_application_id ON application_events(application_id);

-- User preferences
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_roles TEXT,
    target_seniority VARCHAR(50),
    preferred_skills TEXT,
    must_have_skills TEXT,
    nice_to_have_skills TEXT,
    remote_first BOOLEAN DEFAULT TRUE,
    relocation_friendly BOOLEAN DEFAULT FALSE,
    preferred_countries TEXT,
    excluded_countries TEXT,
    excluded_companies TEXT,
    remote_min_salary DECIMAL(12,2),
    relocation_min_salary DECIMAL(12,2),
    nigeria_min_salary DECIMAL(12,2),
    minimum_remote_fit_score DECIMAL(5,2) DEFAULT 75.00,
    minimum_relocation_fit_score DECIMAL(5,2) DEFAULT 70.00,
    minimum_nigeria_fit_score DECIMAL(5,2) DEFAULT 85.00,
    max_applications_per_day INTEGER DEFAULT 10,
    approval_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Audit logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
