/*
================================================================================
   DASH TRIP PLANNER APP - MASTER MIGRATION SCRIPT (v9.1)
================================================================================

   FILE: migration_file_v9.1_SUPABASE_DASH_TRIP_PLANNER_NO_JWT.sql
   DATE: 2026-02-04
   
   OVERVIEW:
   This is the DEFINITIVE, COMPREHENSIVE schema definition for the Dash Trip 
   Planner app. It consolidates all previous migrations and includes complete
   documentation of Edge Functions, specifically noting the disabled JWT
   verification for the AI planner to ensure public accessibility during demo.
   
================================================================================
   KEY FEATURES
================================================================================
   
   1. AI TRIP PLANNING (Hybrid Architecture)
      - On-Device: Gemini Nano for supported devices (Pixel 8/9/10 Pro)
      - Cloud: Gemini 1.5 Flash via Supabase Edge Function
      - Conversation persistence in ai_conversations table
      
   2. REAL-TIME COLLABORATION
      - Multi-user trip editing
      - Live voting on itinerary items
      - Group chat messaging
      
   3. DOCUMENT VAULT
      - Encrypted storage for passports, visas, tickets
      - Category-based organization
      
   4. EXPENSE MANAGEMENT
      - Group expense splitting
      - Settlement tracking
      
================================================================================
   EDGE FUNCTIONS (DEPLOYED TO SUPABASE)
================================================================================
   
   FUNCTION: generate-plan
   PATH: supabase/functions/generate-plan/index.ts
   PURPOSE: AI-powered trip itinerary generation using Google Gemini 1.5 Flash
   CONFIGURATION: verify_jwt: false
   NOTE: JWT verification is DISABLED to allow the Android app to call this 
         function without strict session handling during the demo phase.
   
   REQUIRED SECRETS:
   - GOOGLE_API_KEY: Your Google AI API key (set in Supabase Dashboard > 
                     Edge Functions > Secrets)
   
   REQUEST BODY:
   {
     "prompt": "3 days in Kyoto, food + temples, budget $800",
     "tripId": "uuid-optional",           // If provided, items are saved to trip
     "preferences": {
       "pace": "relaxed|medium|packed",
       "interests": ["food", "culture"],
       "dietary": ["vegetarian"]
     }
   }
   
   RESPONSE:
   {
     "success": true,
     "items": [
       {
         "type": "activity|eat|stay",
         "title": "Visit Fushimi Inari",
         "description": "Thousands of vermillion torii gates...",
         "location_name": "Fushimi Inari Shrine",
         "estimated_cost": 0,
         "currency": "JPY",
         "start_time": "2026-05-01T09:00:00Z",
         "end_time": "2026-05-01T12:00:00Z"
       }
     ]
   }
   
   FUNCTION: send-trip-invite
   PATH: supabase/functions/send-trip-invite/index.ts
   PURPOSE: Send email invitations for trip collaboration
   
================================================================================
   ANDROID APP INTEGRATION
================================================================================
   
   AI REPOSITORY (AIRepository.kt):
   - Implements Strategy pattern for hybrid AI
   - CloudGeminiStrategy: Calls generate-plan Edge Function
   - OnDeviceGeminiStrategy: Uses Gemini Nano (on supported devices)
   
   VIEWMODEL (AIPlannerViewModel.kt):
   - Manages conversation state
   - Persists chats to ai_conversations table
   
   UI (AIPlannerScreen.kt):
   - Conversational interface
   - Thumb-friendly 56dp touch targets
   - Bottom-aligned input for one-handed use
   
================================================================================
   EXECUTION INSTRUCTIONS
================================================================================
   
   1. Run this script in Supabase SQL Editor
   2. Set GOOGLE_API_KEY in Edge Functions > Secrets
   3. Deploy Edge Functions including the no-verify-jwt flag for generate-plan
   
   WARNING: This script DROPS EXISTING TABLES. Back up data if running on 
            production with existing data.
            
================================================================================
*/

-- ============================================================================
-- SECTION 1: CLEANUP (Drop everything to ensure fresh state)
-- ============================================================================

-- Drop triggers first (they depend on functions)
drop trigger if exists on_auth_user_created on auth.users;
drop trigger if exists on_trip_created on public.trips;
drop trigger if exists on_member_update on public.trip_members;

-- Drop functions (cascade removes dependent objects)
drop function if exists public.handle_new_user() cascade;
drop function if exists public.handle_new_trip() cascade;
drop function if exists public.check_member_role_update() cascade;
drop function if exists public.is_trip_member(uuid, text) cascade;
drop function if exists public.is_trip_owner(uuid) cascade; 
drop function if exists public.is_trip_editor(uuid) cascade;
drop function if exists public.has_pending_invite(uuid) cascade;
drop function if exists public.cast_vote(uuid, text) cascade;
drop function if exists public.get_user_stats(uuid) cascade;

-- Drop tables in dependency order (children first)
drop table if exists itinerary_votes cascade;
drop table if exists place_knowledge cascade;
drop table if exists itinerary_attachments cascade;
drop table if exists trip_chat_messages cascade;
drop table if exists ai_conversations cascade;
drop table if exists booking_inbox cascade;
drop table if exists document_vault cascade;
drop table if exists expense_splits cascade;
drop table if exists expenses cascade;
drop table if exists itinerary_items cascade;
drop table if exists trip_members cascade;
drop table if exists price_alerts cascade;
drop table if exists user_relationships cascade; 
drop table if exists trips cascade;
drop table if exists profiles cascade;

-- Drop custom enums
drop type if exists trip_role cascade;
drop type if exists itinerary_status cascade;
drop type if exists doc_category cascade;
drop type if exists member_status cascade;
drop type if exists trip_visibility cascade;
drop type if exists subscription_tier cascade;

-- ============================================================================
-- SECTION 2: EXTENSIONS
-- ============================================================================
-- Required PostgreSQL extensions for advanced functionality

-- UUID generation (for primary keys)
create extension if not exists "uuid-ossp" with schema "extensions";

-- Vector embeddings (for AI semantic search)
create extension if not exists "vector" with schema "extensions";

-- PostGIS (for geospatial queries and location data)
create extension if not exists "postgis" with schema "extensions";

-- Case-insensitive text (for email matching)
do $$
begin
    if exists (select 1 from pg_extension where extname = 'citext') then
        alter extension citext set schema extensions;
    else
        create extension "citext" with schema "extensions";
    end if;
end $$;

-- ============================================================================
-- SECTION 3: CUSTOM ENUM TYPES
-- ============================================================================

-- Role hierarchy for trip collaboration (owner > editor > viewer)
create type trip_role as enum ('owner', 'editor', 'viewer');

-- Itinerary item lifecycle states
-- 'proposed' = AI-generated, pending user approval
-- 'confirmed' = User-approved activity
-- 'draft' = Work in progress
-- 'archived' = Completed or cancelled
create type itinerary_status as enum ('draft', 'confirmed', 'proposed', 'archived');

-- Document categories for the vault feature
create type doc_category as enum ('passport', 'visa', 'ticket', 'insurance', 'health', 'other');

-- Invitation status for trip collaboration
create type member_status as enum ('pending', 'accepted', 'declined');

-- Trip visibility for social features
create type trip_visibility as enum ('private', 'friends', 'public');

-- Subscription tiers for premium features
create type subscription_tier as enum ('free', 'pro');

-- ============================================================================
-- SECTION 4: TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PROFILES: Core user data (extends auth.users)
-- ----------------------------------------------------------------------------
-- Auto-populated via trigger when user signs up
create table profiles (
    id uuid references auth.users on delete cascade primary key,
    username text unique,                              -- Display name (unique)
    full_name text,                                    -- Real name from OAuth
    avatar_url text,                                   -- Profile picture URL
    bio text,                                          -- User description
    birth_date date,                                   -- For age-related features
    gender text,                                       -- Optional demographic
    home_country_code text,                            -- ISO 3166-1 alpha-2 code
    base_currency varchar(3) default 'USD',            -- Preferred currency
    last_seen timestamp with time zone,                -- Activity tracking
    preferences jsonb default '{}',                    -- App preferences blob
    theme_preference text default 'system',            -- 'light', 'dark', 'system'
    language_preference text default 'en',             -- ISO 639-1 code
    calendar_sync_settings jsonb default '{}',         -- Google/Apple calendar config
    subscription_tier subscription_tier default 'free', -- Pro unlocks premium features
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- USER RELATIONSHIPS: Following/Followers for social features
-- ----------------------------------------------------------------------------
create table user_relationships (
    follower_id uuid references profiles(id) on delete cascade,
    following_id uuid references profiles(id) on delete cascade,
    created_at timestamp with time zone default now(),
    primary key (follower_id, following_id)
);

-- ----------------------------------------------------------------------------
-- TRIPS: High-level trip containers
-- ----------------------------------------------------------------------------
-- Central entity that groups all trip-related data
create table trips (
    id uuid default uuid_generate_v4() primary key,
    created_by uuid references profiles(id) on delete set null,
    title text not null,                               -- Trip name (e.g., "Japan 2026")
    description text,                                  -- Trip overview
    start_date date,                                   -- Departure date
    end_date date,                                     -- Return date
    timezone text default 'UTC',                       -- Display timezone
    destination_data jsonb default '{}',               -- {name, lat, lng, country}
    origin_data jsonb default '{}',                    -- Departure location
    visibility trip_visibility default 'private',      -- Social sharing setting
    share_token uuid default uuid_generate_v4(),       -- Public share link token
    is_template boolean default false,                 -- Trip template flag
    is_public boolean default false,                   -- Discoverable in search
    trip_image_url text,                               -- Cover photo
    custom_attributes jsonb default '{}',              -- Extensible metadata
    budget_limit decimal(12, 2),                       -- Total budget cap
    display_order int default 0,                       -- UI sorting order
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- TRIP MEMBERS: Collaboration management
-- ----------------------------------------------------------------------------
-- Links users to trips with role-based permissions
create table trip_members (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    invited_email extensions.citext,                   -- Email for pending invites
    role trip_role default 'viewer',                   -- Permission level
    status member_status default 'pending',            -- Invitation status
    member_origin_data jsonb default '{}',             -- Member's travel origin
    unique(trip_id, user_id),
    -- Owners must always be 'accepted' (constraint prevents demoting without accepting)
    constraint check_owner_is_accepted check (role != 'owner' or status = 'accepted')
);
-- Index for fast email-based invite lookups
create index idx_trip_members_invited_email on trip_members(invited_email);
-- Required for Supabase Realtime to track changes
alter table trip_members replica identity full;

-- ----------------------------------------------------------------------------
-- ITINERARY ITEMS: Individual activities, meals, stays
-- ----------------------------------------------------------------------------
-- Core trip content - what users will actually do
-- AI-generated items have status = 'proposed' until confirmed
create table itinerary_items (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    type text not null,                                -- 'activity', 'eat', 'stay', 'flight'
    status itinerary_status default 'confirmed',       -- Item state
    title text not null,                               -- Display name
    description text,                                  -- Details
    start_time timestamp with time zone,               -- Scheduled start
    end_time timestamp with time zone,                 -- Scheduled end
    location_name text,                                -- Place name
    location_coords extensions.geography(POINT),       -- PostGIS point for mapping
    location_lat double precision,                     -- Latitude (for simple clients)
    location_lng double precision,                     -- Longitude (for simple clients)
    location_google_place_id text,                     -- Google Places ID for enrichment
    estimated_cost decimal(10, 2) default 0.00,        -- Expected cost
    currency varchar(3) default 'USD',                 -- Cost currency
    booking_ref text,                                  -- Confirmation number
    provider_details jsonb default '{}',               -- Booking provider metadata
    realtime_status jsonb default '{}',                -- Live status (delays, etc.)
    sorting_index float8 default 0.0,                  -- Manual ordering within day
    created_at timestamp with time zone default now()
);
create index idx_itin_trip on itinerary_items(trip_id);
alter table itinerary_items replica identity full;

-- ----------------------------------------------------------------------------
-- ITINERARY VOTES: Real-time collaborative decision making
-- ----------------------------------------------------------------------------
-- Allows trip members to vote on proposed activities
create table itinerary_votes (
    id uuid default uuid_generate_v4() primary key,
    itinerary_item_id uuid references itinerary_items(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    vote_type text check (vote_type in ('up', 'down')), -- Vote direction
    weight int default 1,                               -- Vote strength (future use)
    created_at timestamp with time zone default now(),
    unique(itinerary_item_id, user_id)                  -- One vote per user per item
);
alter table itinerary_votes replica identity full;

-- ----------------------------------------------------------------------------
-- ITINERARY ATTACHMENTS: Files related to items (tickets, confirmations)
-- ----------------------------------------------------------------------------
create table itinerary_attachments (
    id uuid default uuid_generate_v4() primary key,
    itinerary_id uuid references itinerary_items(id) on delete cascade,
    file_name text not null,
    file_type text,                                    -- MIME type
    storage_path text not null,                        -- Supabase Storage path
    file_size int,                                     -- Bytes
    user_id uuid references profiles(id),              -- Uploader
    is_shared boolean default true,                    -- Visible to all members
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- EXPENSES: Budget tracking for the trip
-- ----------------------------------------------------------------------------
create table expenses (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    paid_by uuid references profiles(id) on delete set null,
    amount decimal(10, 2) not null,
    currency varchar(3) default 'USD',
    description text,
    category text,                                     -- 'food', 'transport', 'lodging'
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- EXPENSE SPLITS: Shared costs between members
-- ----------------------------------------------------------------------------
create table expense_splits (
    id uuid default uuid_generate_v4() primary key,
    expense_id uuid references expenses(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    amount_owed decimal(10, 2),
    is_settled boolean default false                   -- Paid back?
);

-- ----------------------------------------------------------------------------
-- DOCUMENT VAULT: Secure storage for passports, visas, etc.
-- ----------------------------------------------------------------------------
create table document_vault (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    file_name text,
    storage_path text,                                 -- Supabase Storage path
    doc_type doc_category default 'other',             -- Document classification
    is_encrypted boolean default true,                 -- Client-side encryption flag
    expiry_date date,                                  -- Document expiration
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- BOOKING INBOX: Email parsing for automatic trip building
-- ----------------------------------------------------------------------------
create table booking_inbox (
    id uuid default uuid_generate_v4() primary key,
    user_id uuid references profiles(id) on delete cascade,
    raw_email_body text,                               -- Original email content
    parsing_status text default 'pending',             -- 'pending', 'parsed', 'failed'
    extracted_data jsonb,                              -- Parsed booking details
    source text,                                       -- 'gmail', 'forwarded'
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- AI CONVERSATIONS: History of AI Planning chats
-- ----------------------------------------------------------------------------
-- Stores conversation history for the AI trip planner feature
-- Used by AIPlannerViewModel to persist chat sessions
create table ai_conversations (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,  -- Optional trip association
    user_id uuid references profiles(id) on delete cascade,
    messages jsonb default '[]',                           -- Array of {role, content}
    -- Example: [{"role": "user", "content": "Plan 3 days in Tokyo"},
    --           {"role": "assistant", "content": "Here's your itinerary..."}]
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- TRIP CHAT: Human-to-Human real-time messaging
-- ----------------------------------------------------------------------------
create table trip_chat_messages (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    content text not null,
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- PRICE ALERTS: Tracking flight/hotel prices
-- ----------------------------------------------------------------------------
create table price_alerts (
    id uuid default uuid_generate_v4() primary key,
    user_id uuid references profiles(id) on delete cascade,
    search_params jsonb not null,                      -- Flight/hotel search criteria
    target_price decimal(10, 2),                       -- Alert threshold
    current_price decimal(10, 2),                      -- Last checked price
    is_active boolean default true,
    created_at timestamp with time zone default now()
);

-- ----------------------------------------------------------------------------
-- PLACE KNOWLEDGE: Semantic database for AI lookups
-- ----------------------------------------------------------------------------
-- Vector embeddings for AI-powered place recommendations
create table place_knowledge (
    id uuid default uuid_generate_v4() primary key,
    place_name text not null,
    description text,
    category text,                                     -- 'restaurant', 'attraction', etc.
    location_coords extensions.geography(POINT),
    embedding extensions.vector(1536),                 -- OpenAI/Gemini embeddings
    metadata jsonb default '{}',
    source text,                                       -- 'google_places', 'user', 'ai'
    created_at timestamp with time zone default now()
);

-- ============================================================================
-- SECTION 5: STORAGE BUCKETS
-- ============================================================================

-- Create private bucket for document storage
insert into storage.buckets (id, name, public)
values ('documents', 'documents', false)
on conflict (id) do nothing;

-- ============================================================================
-- SECTION 6: FUNCTIONS & TRIGGERS
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Auto-create profile when user signs up
-- ----------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.profiles (id, full_name, username)
    values (
        new.id,
        coalesce(new.raw_user_meta_data->>'full_name', ''),
        coalesce(new.email, 'user_' || substr(new.id::text, 1, 8))
    );
    -- Link any pending invites to the new user by email
    update public.trip_members
    set user_id = new.id
    where invited_email = new.email;
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

-- ----------------------------------------------------------------------------
-- Auto-add owner as accepted member when trip is created
-- ----------------------------------------------------------------------------
create or replace function public.handle_new_trip()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.trip_members (trip_id, user_id, role, status)
    values (new.id, new.created_by, 'owner', 'accepted')
    on conflict (trip_id, user_id) do nothing;
    return new;
end;
$$;

create trigger on_trip_created
    after insert on public.trips
    for each row execute procedure public.handle_new_trip();

-- ----------------------------------------------------------------------------
-- Enforce owner-only role modifications
-- ----------------------------------------------------------------------------
-- Prevents non-owners from changing member roles
create or replace function public.check_member_role_update()
returns trigger language plpgsql security definer set search_path = public, auth as $$
declare
    _is_owner boolean;
begin
    if old.role is distinct from new.role then
        select exists (
            select 1 from trip_members
            where trip_id = new.trip_id
            and user_id = auth.uid()
            and role = 'owner'
        ) into _is_owner;
        if not _is_owner then
            raise exception 'Only trip owners can change member roles.';
        end if;
    end if;
    return new;
end;
$$;

create trigger on_member_update
    before update on public.trip_members
    for each row execute procedure public.check_member_role_update();

-- ----------------------------------------------------------------------------
-- RLS Helper Functions
-- ----------------------------------------------------------------------------

-- Check if current user is a trip member with required role
create or replace function public.is_trip_member(_trip_id uuid, _req_role text default 'viewer')
returns boolean language sql security definer set search_path = public, auth as $$
    select exists (
        select 1 from trip_members
        where trip_id = _trip_id
        and user_id = auth.uid()
        and status = 'accepted' 
        and (
            _req_role = 'viewer'
            or (_req_role = 'editor' and role in ('owner', 'editor'))
            or (_req_role = 'owner' and role = 'owner')
        )
    );
$$;

-- Check if current user is the trip owner
create or replace function public.is_trip_owner(_trip_id uuid)
returns boolean language sql security definer set search_path = public, auth as $$
    select exists (
        select 1 from trip_members
        where trip_id = _trip_id
        and user_id = auth.uid()
        and role = 'owner'
    );
$$;

-- Check if current user can edit the trip
create or replace function public.is_trip_editor(_trip_id uuid)
returns boolean language sql security definer set search_path = public, auth as $$
    select exists (
        select 1 from trip_members
        where trip_id = _trip_id
        and user_id = auth.uid()
        and status = 'accepted'
        and role in ('owner', 'editor')
    );
$$;

-- Check if current user has a pending invite for this trip
create or replace function public.has_pending_invite(_trip_id uuid)
returns boolean language sql security definer set search_path = public, auth as $$
    select exists (
        select 1 from trip_members
        where trip_id = _trip_id
        and (user_id = auth.uid() or invited_email = auth.jwt() ->> 'email')
        and status = 'pending'
    );
$$;

-- ----------------------------------------------------------------------------
-- User Statistics RPC (called from ProfileScreen)
-- ----------------------------------------------------------------------------
create or replace function public.get_user_stats(target_user_id uuid)
returns json
language plpgsql
security definer
as $$
declare
  trip_count integer;
  place_count integer;
begin
  -- Count trips user is member of
  select count(*) into trip_count
  from trip_members
  where user_id = target_user_id and status = 'accepted';

  -- Count distinct locations visited
  select count(distinct ii.location_name) into place_count
  from itinerary_items ii
  join trips t on t.id = ii.trip_id
  join trip_members tm on tm.trip_id = t.id
  where tm.user_id = target_user_id
  and tm.status = 'accepted'
  and ii.location_name is not null
  and ii.location_name != '';

  return json_build_object(
    'trip_count', trip_count,
    'place_count', place_count
  );
end;
$$;

-- ----------------------------------------------------------------------------
-- Voting RPC (upsert pattern)
-- ----------------------------------------------------------------------------
create or replace function public.cast_vote(
    _item_id uuid,
    _vote_type text
) returns json language plpgsql security definer set search_path = public, auth as $$
declare
    _result json;
begin
    insert into itinerary_votes (itinerary_item_id, user_id, vote_type)
    values (_item_id, auth.uid(), _vote_type)
    on conflict (itinerary_item_id, user_id)
    do update set vote_type = excluded.vote_type, created_at = now()
    returning row_to_json(itinerary_votes.*) into _result;
    
    return _result;
end;
$$;
grant execute on function public.cast_vote(uuid, text) to authenticated;


-- ============================================================================
-- SECTION 7: ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on all tables
alter table profiles enable row level security;
alter table trips enable row level security;
alter table trip_members enable row level security;
alter table itinerary_items enable row level security;
alter table itinerary_votes enable row level security;
alter table itinerary_attachments enable row level security;
alter table expenses enable row level security;
alter table expense_splits enable row level security;
alter table document_vault enable row level security;
alter table booking_inbox enable row level security;
alter table ai_conversations enable row level security;
alter table trip_chat_messages enable row level security;
alter table price_alerts enable row level security;
alter table place_knowledge enable row level security;
alter table user_relationships enable row level security;

-- PROFILES
create policy "Public profiles" on profiles for select using (true);
create policy "Update self" on profiles for update using (auth.uid() = id);
create policy "Insert self" on profiles for insert with check (auth.uid() = id);

-- TRIPS
create policy "View trips" on trips for select using (
    created_by = auth.uid()             -- Owner always sees
    or is_trip_member(id)               -- Accepted members see
    or has_pending_invite(id)           -- Pending invitees see
    or visibility = 'public'            -- Public trips visible to all
);
create policy "Create trips" on trips for insert with check (auth.uid() = created_by);
create policy "Edit trips" on trips for update using (is_trip_editor(id));
create policy "Delete trips" on trips for delete using (created_by = auth.uid());

-- TRIP MEMBERS
create policy "View members" on trip_members for select using (
    user_id = auth.uid()
    or invited_email = auth.jwt() ->> 'email'
    or is_trip_owner(trip_id)
    or is_trip_member(trip_id) 
);
create policy "Add members" on trip_members for insert with check (is_trip_owner(trip_id));
create policy "Update member roles" on trip_members for update using (is_trip_owner(trip_id));
create policy "Manage self status" on trip_members for update using (
    user_id = auth.uid() or (invited_email = auth.jwt() ->> 'email' and status = 'pending')
);
create policy "Remove members" on trip_members for delete using (
    is_trip_owner(trip_id) or user_id = auth.uid()
);

-- ITINERARY ITEMS
create policy "Items view" on itinerary_items for select using (
    is_trip_owner(trip_id) or is_trip_member(trip_id, 'viewer')
);
create policy "Items insert" on itinerary_items for insert with check (is_trip_editor(trip_id));
create policy "Items update" on itinerary_items for update using (is_trip_editor(trip_id));
create policy "Items delete" on itinerary_items for delete using (is_trip_editor(trip_id));

-- ITINERARY VOTES
create policy "Vote select" on itinerary_votes for select using (true);
create policy "Vote insert" on itinerary_votes for insert with check (user_id = auth.uid());
create policy "Vote update" on itinerary_votes for update using (user_id = auth.uid());
create policy "Vote delete" on itinerary_votes for delete using (user_id = auth.uid());

-- ATTACHMENTS
create policy "Insert attachments" on itinerary_attachments for insert with check (auth.uid() = user_id);
create policy "View attachments" on itinerary_attachments for select using (true);
create policy "Delete attachments" on itinerary_attachments for delete using (auth.uid() = user_id);

-- DOCUMENT VAULT
create policy "Vault owner access" on document_vault for all using (user_id = auth.uid());
create policy "Vault shared view" on document_vault for select using (is_trip_member(trip_id, 'viewer'));

-- AI CONVERSATIONS
create policy "View AI chats" on ai_conversations for select using (user_id = auth.uid());
create policy "Manage AI chats" on ai_conversations for all using (user_id = auth.uid());

-- TRIP CHAT
create policy "View chat messages" on trip_chat_messages for select using (is_trip_member(trip_id, 'viewer'));
create policy "Send chat messages" on trip_chat_messages for insert with check (is_trip_member(trip_id, 'viewer') AND user_id = auth.uid());

-- EXPENSES
create policy "View expenses" on expenses for select using (is_trip_member(trip_id, 'viewer'));
create policy "Manage expenses" on expenses for all using (is_trip_editor(trip_id));
create policy "View splits" on expense_splits for select using (exists (select 1 from expenses where id = expense_id and is_trip_member(trip_id, 'viewer')));
create policy "Manage splits" on expense_splits for all using (exists (select 1 from expenses where id = expense_id and is_trip_editor(trip_id)));

-- BOOKING INBOX & PRICE ALERTS
create policy "Manage inbox" on booking_inbox for all using (user_id = auth.uid());
create policy "Manage alerts" on price_alerts for all using (user_id = auth.uid());

-- PLACE KNOWLEDGE (public read, pro write)
create policy "Read places" on place_knowledge for select using (true);
create policy "Manage places" on place_knowledge for all using (auth.uid() IN (SELECT id FROM profiles WHERE subscription_tier = 'pro'));

-- USER RELATIONSHIPS
create policy "View relationships" on user_relationships for select using (true);
create policy "Manage relationships" on user_relationships for all using (follower_id = auth.uid());

-- STORAGE POLICIES
drop policy if exists "Upload docs" on storage.objects;
create policy "Upload docs" on storage.objects for insert to authenticated with check (bucket_id = 'documents');
drop policy if exists "View docs" on storage.objects;
create policy "View docs" on storage.objects for select to authenticated using (bucket_id = 'documents');

-- ============================================================================
-- SECTION 8: REALTIME CONFIGURATION
-- ============================================================================
-- Enable Supabase Realtime for collaborative features

alter publication supabase_realtime add table trips;
alter publication supabase_realtime add table itinerary_items;
alter publication supabase_realtime add table itinerary_votes;
alter publication supabase_realtime add table trip_members;
alter publication supabase_realtime add table trip_chat_messages;

-- ============================================================================
-- END OF MIGRATION v9.1
-- ============================================================================
/*
   DEPLOYMENT CHECKLIST:
   
   [ ] 1. Run this migration in Supabase SQL Editor
   [ ] 2. Set GOOGLE_API_KEY in Dashboard > Edge Functions > Secrets
   [ ] 3. Deploy Edge Functions:
         supabase functions deploy generate-plan --no-verify-jwt
         supabase functions deploy send-trip-invite
   [ ] 4. Update local.properties with correct SUPABASE_URL and SUPABASE_KEY
   [ ] 5. Build and deploy Android app
   
   TROUBLESHOOTING:
   
   Q: "Requested function not found" error
   A: Edge function not deployed. Check Dashboard > Edge Functions
   
   Q: "AI Generation failed" error  
   A: GOOGLE_API_KEY secret not set or invalid
   
   Q: RLS blocking queries
   A: Ensure user is authenticated and has appropriate role
*/
