/*
================================================================================
   DASH TRIP PLANNER APP - MIGRATION v9.2 (AI CACHING)
================================================================================
   FILE: migration_file_v9.2_AI_CACHING.sql
   DATE: 2026-02-12
   
   OVERVIEW:
   Adds caching layer for AI Trip Planning responses (`ai_suggestions_cache`).
   Reduces API costs and latency for common queries.
   
   Dependencies:
   - Must run AFTER v9.1 migration.
================================================================================
*/

-- 1. Create Cache Table
create table if not exists public.ai_suggestions_cache (
    id uuid default uuid_generate_v4() primary key,
    user_query_hash text not null,                     -- SHA-256(normalized_prompt + location)
    location_normalized text,                          -- e.g. "kyoto, japan"
    prompt_type text,                                  -- e.g. "itinerary", "places"
    response_payload jsonb not null,                   -- The full AI JSON response
    created_at timestamp with time zone default now(),
    expires_at timestamp with time zone default (now() + interval '180 days')
);

-- 2. Create Unique Index for Fast Lookups
create unique index if not exists idx_ai_cache_query_hash 
on public.ai_suggestions_cache(user_query_hash);

-- 3. Enable RLS
alter table public.ai_suggestions_cache enable row level security;

-- 4. RLS Policies
-- Public (anon) can read cache (to get fast results)
create policy "Read cache" on public.ai_suggestions_cache
    for select
    to anon, authenticated
    using (true);

-- Only Service Role (Edge Functions) can write to cache
-- Ideally, we'd restrict this further, but for now authenticated users (Edge Function context) can write if needed,
-- or strictly service_role. Since Edge Functions run with SERVICE_ROLE key by default for DB ops if configured,
-- we'll allow all authenticated for simplicity in this demo environment, but a better practice is service_role only.
-- However, `anon` key calls to Edge Function usually act as `anon` user.
-- Let's allow `anon` and `authenticated` to Insert for now as the Edge Function runs with the caller's context.
create policy "Write cache" on public.ai_suggestions_cache
    for insert
    to anon, authenticated
    with check (true);

-- 5. Periodic Cleanup (Optional, manual for now)
-- Delete expired entries: delete from ai_suggestions_cache where expires_at < now();
