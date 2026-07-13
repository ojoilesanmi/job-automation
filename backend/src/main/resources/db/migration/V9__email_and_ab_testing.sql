CREATE TABLE IF NOT EXISTS ab_test_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    experiment_name VARCHAR(100) NOT NULL,
    variant VARCHAR(50) NOT NULL,
    outcome VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_ab_test_results_user_experiment ON ab_test_results(user_id, experiment_name);
