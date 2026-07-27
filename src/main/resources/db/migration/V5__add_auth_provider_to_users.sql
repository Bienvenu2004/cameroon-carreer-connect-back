-- Track how each account authenticates so Google Sign-In accounts are
-- distinguishable from classic email+password accounts. Existing rows are
-- assumed LOCAL. Google accounts are written as 'GOOGLE' by the app layer.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
