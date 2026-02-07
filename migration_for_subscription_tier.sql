-- Add subscription_tier column to profiles table
-- This enables Feature Gating for Pro/Free users

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS subscription_tier text DEFAULT 'free';

-- Update existing rows to 'free' if null
UPDATE public.profiles 
SET subscription_tier = 'free' 
WHERE subscription_tier IS NULL;
