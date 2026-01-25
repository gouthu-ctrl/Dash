/*
========================================================================
   TRIP PLANNER APP - MASTER MIGRATION SCRIPT (v3.6)
========================================================================
   V3.6 UPDATES:
   - Added 'is_shared' column to itinerary_attachments for privacy control.
   - Standardized security policies for file attachments.
========================================================================
*/

-- =====================================================================
-- 1. CLEANUP PHASE
-- =====================================================================

drop trigger if exists on_auth_user_created on auth.users;
drop trigger if exists on_trip_created on public.trips;
drop function if exists public.handle_new_user() cascade;
drop function if exists public.handle_new_trip() cascade;
drop function if exists public.is_trip_member(uuid) cascade;
drop function if exists public.is_trip_member(uuid, text) cascade;

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

-- =====================================================================
-- 2. SETUP & EXTENSIONS
-- =====================================================================

create extension if not exists "uuid-ossp" with schema "extensions";
create extension if not exists "vector" with schema "extensions";
create extension if not exists "postgis" with schema "extensions";

create type trip_role as enum ('owner', 'editor', 'viewer');
create type itinerary_status as enum ('draft', 'confirmed', 'proposed', 'archived');
create type doc_category as enum ('passport', 'visa', 'ticket', 'insurance', 'other');
create type member_status as enum ('pending', 'accepted', 'declined');

-- =====================================================================
-- 3. TABLE DEFINITIONS
-- =====================================================================

create table profiles (
    id uuid references auth.users on delete cascade primary key,
    username text unique,
    full_name text,
    avatar_url text,
    bio text,
    birth_date date,
    gender text check (gender in ('male', 'female', 'non_binary', 'other', 'prefer_not_to_say')),
    home_country_code varchar(2),
    base_currency varchar(3) default 'USD',
    last_seen timestamp with time zone,
    preferences jsonb default '{"pace": "medium", "interests": [], "dietary": []}',
    calendar_sync_settings jsonb default '{"google_sync": false, "device_sync": false}',
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
    is_template boolean default false,
    is_public boolean default false,
    template_price decimal(10, 2) default 0.00,
    template_tags text[],
    share_token uuid default uuid_generate_v4(),
    widgets_config jsonb default '[]',
    trip_image_url text,
	custom_attributes jsonb default '{}',
    budget_limit decimal(12, 2),
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

create table trip_members (
    id uuid default uuid_generate_v4() primary key,
    trip_id uuid references trips(id) on delete cascade,
    user_id uuid references profiles(id) on delete cascade,
    invited_email text,
    role trip_role default 'viewer',
    status member_status default 'pending',
    member_origin_data jsonb default '{}',
    unique(trip_id, user_id),
    constraint check_owner_is_accepted check (role != 'owner' or status = 'accepted')
);

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
    location_google_place_id text,
    estimated_cost decimal(10, 2) default 0.00,
    currency varchar(3) default 'USD',
    booking_ref text,
    provider_details jsonb default '{}',
    sorting_index float8,
    created_at timestamp with time zone default now()
);

create table itinerary_attachments (
    id uuid default uuid_generate_v4() primary key,
    itinerary_id uuid references itinerary_items(id) on delete cascade,
    file_name text not null,
    file_type text,
    storage_path text not null,
    file_size int,
    user_id uuid references profiles(id),
    is_shared boolean default true, -- v3.6 added
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
    file_name text not null,
    storage_path text not null,
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
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
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

-- =====================================================================
-- 4. AUTOMATION
-- =====================================================================

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, avatar_url, username)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''),
    coalesce(new.raw_user_meta_data->>'avatar_url', new.raw_user_meta_data->>'picture', ''),
    coalesce(new.email, 'user_' || substr(new.id::text, 1, 8))
  );
  return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

create or replace function public.handle_new_trip()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.trip_members (trip_id, user_id, role, status)
  values (new.id, new.created_by, 'owner', 'accepted');
  return new;
end;
$$;

create trigger on_trip_created
    after insert on public.trips
    for each row execute procedure public.handle_new_trip();

create or replace function public.is_trip_member(_trip_id uuid, _req_role text default 'viewer')
returns boolean
language sql
security definer
set search_path = public, auth
as $$
  select exists (
    select 1 from trip_members
    where trip_id = _trip_id
    and user_id = auth.uid()
    and (
      _req_role = 'viewer'
      or (_req_role = 'editor' and role in ('owner', 'editor'))
      or (_req_role = 'owner' and role = 'owner')
    )
  );
$$;

-- =====================================================================
-- 5. SECURITY (RLS)
-- =====================================================================

alter table profiles enable row level security;
alter table user_relationships enable row level security;
alter table trips enable row level security;
alter table trip_members enable row level security;
alter table itinerary_items enable row level security;
alter table itinerary_attachments enable row level security;
alter table expenses enable row level security;
alter table expense_splits enable row level security;
alter table document_vault enable row level security;
alter table booking_inbox enable row level security;
alter table ai_conversations enable row level security;
alter table price_alerts enable row level security;
alter table trip_chat_messages enable row level security;

create policy "Profiles are public" on profiles for select using (true);
create policy "Users manage own profile" on profiles for update using (auth.uid() = id);

create policy "View trips" on trips
    for select using (
        created_by = auth.uid()
        or is_trip_member(id, 'viewer')
        or (is_template = true and is_public = true)
    );
create policy "Create trips" on trips
    for insert with check (auth.uid() = created_by);
create policy "Edit trips" on trips
    for update using (is_trip_member(id, 'editor'));
create policy "Delete trips" on trips
    for delete using (is_trip_member(id, 'owner'));

create policy "View memberships" on trip_members
    for select using (
        user_id = auth.uid()
        or exists (
            select 1 from trips t
            where t.id = trip_id and t.created_by = auth.uid()
        )
    );
create policy "Insert membership" on trip_members
    for insert with check (
        user_id = auth.uid()
        or exists (
            select 1 from trips t
            where t.id = trip_id and t.created_by = auth.uid()
        )
    );
create policy "Update membership" on trip_members
    for update using (
        user_id = auth.uid()
        or exists (
            select 1 from trips t
            where t.id = trip_id and t.created_by = auth.uid()
        )
    );

create policy "View itinerary" on itinerary_items for select using (is_trip_member(trip_id));
create policy "Manage itinerary" on itinerary_items for all using (is_trip_member(trip_id, 'editor'));

-- Attachment Policies (v3.6)
create policy "Trip members can view shared files" on itinerary_attachments
for select using (
    user_id = auth.uid()
    or (is_shared = true and is_trip_member((select trip_id from itinerary_items where id = itinerary_id)))
);

create policy "Users manage own attachments" on itinerary_attachments
for all using (user_id = auth.uid());

create policy "View expenses" on expenses for select using (is_trip_member(trip_id));
create policy "Manage expenses" on expenses for all using (is_trip_member(trip_id, 'editor'));
create policy "Chat access" on trip_chat_messages for all using (is_trip_member(trip_id));
create policy "AI context access" on ai_conversations for all using (is_trip_member(trip_id));
create policy "Private vault" on document_vault for all using (user_id = auth.uid());
create policy "Private inbox" on booking_inbox for all using (user_id = auth.uid());
create policy "Private alerts" on price_alerts for all using (user_id = auth.uid());
