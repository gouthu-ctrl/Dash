/*
========================================================================
   DASH TRIP PLANNER APP - MASTER MIGRATION SCRIPT (v6.1)
========================================================================
   
   PATH: d:\StudioProjects\Dash\migration_file_v6.1_supabase_dash_trip_planner_complete.sql
   
   OVERVIEW:
   Fixes "infinite recursion detected" error.
   
    KEY CHANGES (v6.1):
   - POLICY "View trips": Replaced recursive `exists` with `is_trip_member(id) or has_pending_invite(id)`.
   - POLICY "View members": Replaced recursive `exists` team check with `is_trip_member(trip_id)`.
   
   EXECUTION:
   Run in Supabase SQL Editor.
========================================================================
*/

-- 1. CLEANUP
drop trigger if exists on_auth_user_created on auth.users;
drop trigger if exists on_trip_created on public.trips;
drop function if exists public.handle_new_user() cascade;
drop function if exists public.handle_new_trip() cascade;
drop function if exists public.is_trip_member(uuid, text) cascade;
drop function if exists public.is_trip_owner(uuid) cascade; 
drop function if exists public.has_pending_invite(uuid) cascade;
drop function if exists public.get_user_trip_status(uuid) cascade;

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

drop type if exists trip_role cascade;
drop type if exists itinerary_status cascade;
drop type if exists doc_category cascade;
drop type if exists member_status cascade;
drop type if exists trip_visibility cascade;
drop type if exists subscription_tier cascade;

-- 2. EXTENSIONS
create extension if not exists "uuid-ossp" with schema "extensions";
create extension if not exists "vector" with schema "extensions";
create extension if not exists "postgis" with schema "extensions";

do $$
begin
    if exists (select 1 from pg_extension where extname = 'citext') then
        alter extension citext set schema extensions;
    else
        create extension "citext" with schema "extensions";
    end if;
end $$;

-- 3. ENUMS
create type trip_role as enum ('owner', 'editor', 'viewer');
create type itinerary_status as enum ('draft', 'confirmed', 'proposed', 'archived');
create type doc_category as enum ('passport', 'visa', 'ticket', 'insurance', 'health', 'other');
create type member_status as enum ('pending', 'accepted', 'declined');
create type trip_visibility as enum ('private', 'friends', 'public');
create type subscription_tier as enum ('free', 'pro');

-- 4. TABLES (Same as v6.0)
create table profiles (
    id uuid references auth.users on delete cascade primary key,
    username text unique,
    full_name text,
    avatar_url text,
    bio text,
    birth_date date,
    gender text,
    home_country_code text,
    base_currency varchar(3) default 'USD',
    last_seen timestamp with time zone,
    preferences jsonb default '{}',
    calendar_sync_settings jsonb default '{}',
    subscription_tier subscription_tier default 'free',
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

create table user_relationships (
    follower_id uuid references profiles(id) on delete cascade,
    following_id uuid references profiles(id) on delete cascade,
    created_at timestamp with time zone default now(),
    primary key (follower_id, following_id)
);

create table trips (
    id uuid default uuid_generate_v4() primary key,
    created_by uuid references profiles(id) on delete set null,
    title text not null,
    description text,
    start_date date,
    end_date date,
    timezone text default 'UTC',
    destination_data jsonb default '{}',
    origin_data jsonb default '{}',
    visibility trip_visibility default 'private',
    share_token uuid default uuid_generate_v4(),
    is_template boolean default false,
    is_public boolean default false,
    trip_image_url text,
    custom_attributes jsonb default '{}',
    budget_limit decimal(12, 2),
    display_order int default 0,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

create table trip_members (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    invited_email extensions.citext,
    role trip_role default 'viewer',
    status member_status default 'pending',
    member_origin_data jsonb default '{}',
    unique(trip_id, user_id),
    constraint check_owner_is_accepted check (role != 'owner' or status = 'accepted')
);
create index idx_trip_members_invited_email on trip_members(invited_email);
alter table trip_members replica identity full;

create table itinerary_items (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    type text not null,
    status itinerary_status default 'confirmed',
    title text not null,
    description text,
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    location_name text,
    location_coords extensions.geography(POINT),
    location_lat double precision,
    location_lng double precision,
    location_google_place_id text,
    estimated_cost decimal(10, 2) default 0.00,
    currency varchar(3) default 'USD',
    booking_ref text,
    provider_details jsonb default '{}',
    realtime_status jsonb default '{}',
    sorting_index float8 default 0.0,
    created_at timestamp with time zone default now()
);
create index idx_itin_trip on itinerary_items(trip_id);
alter table itinerary_items replica identity full;

create table itinerary_votes (
    id uuid default uuid_generate_v4() primary key,
    itinerary_item_id uuid references itinerary_items(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    vote_type text,
    weight int default 1,
    created_at timestamp with time zone default now(),
    unique(itinerary_item_id, user_id)
);
alter table itinerary_votes replica identity full;

create table itinerary_attachments (
    id uuid default uuid_generate_v4() primary key,
    itinerary_id uuid references itinerary_items(id) on delete cascade,
    file_name text not null,
    file_type text,
    storage_path text not null,
    file_size int,
    user_id uuid references profiles(id),
    is_shared boolean default true,
    created_at timestamp with time zone default now()
);

create table expenses (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    paid_by uuid references profiles(id) on delete set null,
    amount decimal(10, 2) not null,
    currency varchar(3) default 'USD',
    description text,
    category text,
    created_at timestamp with time zone default now()
);

create table expense_splits (
    id uuid default uuid_generate_v4() primary key,
    expense_id uuid references expenses(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    amount_owed decimal(10, 2),
    is_settled boolean default false
);

create table document_vault (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    file_name text,
    storage_path text,
    doc_type doc_category default 'other',
    is_encrypted boolean default true,
    expiry_date date,
    created_at timestamp with time zone default now()
);

create table booking_inbox (
    id uuid default uuid_generate_v4() primary key,
    user_id uuid references profiles(id) on delete cascade,
    raw_email_body text,
    parsing_status text default 'pending',
    extracted_data jsonb,
    source text,
    created_at timestamp with time zone default now()
);

create table ai_conversations (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    messages jsonb default '[]',
    created_at timestamp with time zone default now()
);
create table trip_chat_messages (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    content text not null,
    created_at timestamp with time zone default now()
);

create table price_alerts (
    id uuid default uuid_generate_v4() primary key,
    user_id uuid references profiles(id) on delete cascade,
    search_params jsonb not null,
    target_price decimal(10, 2),
    current_price decimal(10, 2),
    is_active boolean default true,
    created_at timestamp with time zone default now()
);

create table place_knowledge (
    id uuid default uuid_generate_v4() primary key,
    place_name text not null,
    description text,
    category text,
    location_coords extensions.geography(POINT),
    embedding extensions.vector(1536),
    metadata jsonb default '{}',
    source text,
    created_at timestamp with time zone default now()
);

insert into storage.buckets (id, name, public)
values ('documents', 'documents', false)
on conflict (id) do nothing;


-- 5. FUNCTIONS (Security Definer prevents recursion)

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.profiles (id, full_name, username)
    values (
        new.id,
        coalesce(new.raw_user_meta_data->>'full_name', ''),
        coalesce(new.email, 'user_' || substr(new.id::text, 1, 8))
    );
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

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

create or replace function public.get_user_trip_status(_trip_id uuid)
returns member_status language sql security definer set search_path = public, auth as $$
    select status from trip_members
    where trip_id = _trip_id and user_id = auth.uid();
$$;

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

create or replace function public.is_trip_owner(_trip_id uuid)
returns boolean language sql security definer set search_path = public, auth as $$
    select exists (
        select 1 from trip_members
        where trip_id = _trip_id
        and user_id = auth.uid()
        and role = 'owner'
    );
$$;

create or replace function public.has_pending_invite(_trip_id uuid)
returns boolean language sql security definer set search_path = public, auth as $$
    select exists (
        select 1 from trip_members
        where trip_id = _trip_id
        and (user_id = auth.uid() or invited_email = auth.jwt() ->> 'email')
        and status = 'pending'
    );
$$;

-- 6. POLICIES (RLS)

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

-- TRIPS (Fixed Recursion)
create policy "View trips" on trips for select using (
    created_by = auth.uid()             
    or is_trip_member(id)         -- Uses Security Definer (Fixes recursion)
    or has_pending_invite(id)     -- Uses Security Definer
    or visibility = 'public'
);
create policy "Create trips" on trips for insert with check (auth.uid() = created_by);
create policy "Edit trips" on trips for update using (is_trip_member(id, 'editor'));
create policy "Delete trips" on trips for delete using (created_by = auth.uid());

-- TRIP MEMBERS (Fixed Recursion)
create policy "View members" on trip_members for select using (
    user_id = auth.uid()
    or invited_email = auth.jwt() ->> 'email'
    or is_trip_owner(trip_id)
    or is_trip_member(trip_id) -- Team visibility (Security Definer -> No recursion)
);

create policy "Add members" on trip_members for insert with check (
    is_trip_owner(trip_id)
);

create policy "Update member roles" on trip_members for update using (
    is_trip_owner(trip_id)
);

create policy "Manage self status" on trip_members for update using (
    user_id = auth.uid() 
    or invited_email = auth.jwt() ->> 'email'
) with check (
    status in ('accepted', 'declined') 
);

create policy "Remove members" on trip_members for delete using (
    is_trip_owner(trip_id)
    or user_id = auth.uid()
);

-- ITINERARY ITEMS
create policy "View items" on itinerary_items for select using (
    is_trip_owner(trip_id)
    or is_trip_member(trip_id, 'viewer')
);

create policy "Insert items" on itinerary_items for insert with check (
    is_trip_owner(trip_id)
    or is_trip_member(trip_id, 'editor')
);

create policy "Update items" on itinerary_items for update using (
    is_trip_owner(trip_id)
    or is_trip_member(trip_id, 'editor')
);

create policy "Delete items" on itinerary_items for delete using (
    is_trip_owner(trip_id)
    or is_trip_member(trip_id, 'editor')
);

-- ITINERARY ATTACHMENTS
create policy "Insert attachments" on itinerary_attachments for insert with check (auth.uid() = user_id);
create policy "View attachments" on itinerary_attachments for select using (true);
create policy "Delete attachments" on itinerary_attachments for delete using (auth.uid() = user_id);

-- DOCUMENT VAULT
create policy "Vault owner access" on document_vault for all using (user_id = auth.uid());
create policy "Vault shared view" on document_vault for select using (is_trip_member(trip_id, 'viewer'));

-- OTHER TABLES
create policy "Vote select" on itinerary_votes for select using (true);
create policy "Vote mod" on itinerary_votes for all using (user_id = auth.uid());

-- AI & CHAT
create policy "View AI chats" on ai_conversations for select using (user_id = auth.uid());
create policy "Manage AI chats" on ai_conversations for all using (user_id = auth.uid());

create policy "View chat messages" on trip_chat_messages for select using (
    is_trip_member(trip_id, 'viewer')
);
create policy "Send chat messages" on trip_chat_messages for insert with check (
    is_trip_member(trip_id, 'viewer') AND user_id = auth.uid()
);

-- EXPENSES
create policy "View expenses" on expenses for select using (
    is_trip_member(trip_id, 'viewer')
);
create policy "Manage expenses" on expenses for all using (
    is_trip_member(trip_id, 'editor')
);

create policy "View splits" on expense_splits for select using (
    exists (select 1 from expenses where id = expense_id and is_trip_member(trip_id, 'viewer'))
);
create policy "Manage splits" on expense_splits for all using (
    exists (select 1 from expenses where id = expense_id and is_trip_member(trip_id, 'editor'))
);

-- BOOKING INBOX
create policy "Manage inbox" on booking_inbox for all using (user_id = auth.uid());

-- PRICE ALERTS
create policy "Manage alerts" on price_alerts for all using (user_id = auth.uid());

-- PLACE KNOWLEDGE
create policy "Read places" on place_knowledge for select using (true);
create policy "Manage places" on place_knowledge for all using (auth.uid() IN (SELECT id FROM profiles WHERE subscription_tier = 'pro'));

-- USER RELATIONSHIPS
create policy "View relationships" on user_relationships for select using (true);
create policy "Manage relationships" on user_relationships for all using (follower_id = auth.uid());


-- Storage Policies
drop policy if exists "Upload docs" on storage.objects;
create policy "Upload docs" on storage.objects for insert to authenticated with check (bucket_id = 'documents');

drop policy if exists "View docs" on storage.objects;
create policy "View docs" on storage.objects for select to authenticated using (bucket_id = 'documents');

-- 7. REALTIME
alter publication supabase_realtime add table trips;
alter publication supabase_realtime add table itinerary_items;
alter publication supabase_realtime add table itinerary_votes;
alter publication supabase_realtime add table trip_members;

-- END v6.1 matches v6.0 but with Recursion Fix
