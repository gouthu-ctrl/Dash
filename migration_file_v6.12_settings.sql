-- Migration v6.12: Add Settings to Profile
-- Description: Adds theme_preference and language_preference to profiles table.

-- Add columns if they don't exist
ALTER TABLE profiles 
ADD COLUMN IF NOT EXISTS theme_preference TEXT DEFAULT 'system';

ALTER TABLE profiles 
ADD COLUMN IF NOT EXISTS language_preference TEXT DEFAULT 'en';

-- Comment on columns
COMMENT ON COLUMN profiles.theme_preference IS 'User theme preference: "light", "dark", or "system"';
COMMENT ON COLUMN profiles.language_preference IS 'User language code (ISO 639-1) e.g. "en", "es", "fr"';

-- Update RLS if needed (usually update policy covers all columns, but good to check)
-- Existing policies for UPDATE on profiles typically check 'id = auth.uid()'.
-- No new policies needed if strictly adding columns.
