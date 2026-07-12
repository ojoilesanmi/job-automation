-- V8: Add missing PRD fields

-- Add missing fields to user_preferences
ALTER TABLE user_preferences ADD COLUMN auto_reject_rules TEXT;
ALTER TABLE user_preferences ADD COLUMN excluded_job_levels TEXT;
ALTER TABLE user_preferences ADD COLUMN excluded_industries TEXT;

-- Add missing fields to user_profiles
ALTER TABLE user_profiles ADD COLUMN industries TEXT;
ALTER TABLE user_profiles ADD COLUMN tone_preference TEXT;

-- Add missing field to cv_documents
ALTER TABLE cv_documents ADD COLUMN target_roles TEXT;