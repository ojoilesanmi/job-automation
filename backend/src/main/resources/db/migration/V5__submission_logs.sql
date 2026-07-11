-- V5: Submission logs table

CREATE TABLE submission_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    method VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    error_message TEXT,
    screenshot_path VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_submission_logs_application_id ON submission_logs(application_id);
CREATE INDEX idx_submission_logs_user_id ON submission_logs(user_id);
