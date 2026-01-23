-- Profile table linked to Supabase Auth
create table profiles (
  id uuid references auth.users on delete cascade primary key,
  full_name text,
  avatar_url text,
  home_country_code varchar(2), -- ISO code
  base_currency varchar(3) default 'USD',
  preferences jsonb default '{"pace": "medium", "interests": []}', -- AI uses this
  created_at timestamp with time zone default now()
);

create table trips (
  id uuid default uuid_generate_v4() primary key,
  title text not null,
  description text,
  start_date date,
  end_date date,
  timezone text default 'UTC',
  created_by uuid references profiles(id),
  is_template boolean default false, -- For the Marketplace feature
  share_token uuid default uuid_generate_v4(), -- For "View Only" links
  created_at timestamp with time zone default now()
);

-- For Live Co-editing (Realtime enabled)
create table trip_members (
  id uuid default uuid_generate_v4() primary key,
  trip_id uuid references trips(id) on delete cascade,
  user_id uuid references profiles(id) on delete cascade,
  role text check (role in ('owner', 'editor', 'viewer')),
  unique(trip_id, user_id)
);

-- Enable the PostGIS extension to handle map coordinates
create extension if not exists postgis with schema extensions;

-- (Optional) Ensure the 'extensions' schema is in your search path 
-- so you don't have to type 'extensions.geography' every time
alter database postgres set search_path to "$user", public, extensions;

create table itinerary_items (
  id uuid default uuid_generate_v4() primary key,
  trip_id uuid references trips(id) on delete cascade,
  type text not null, 
  title text not null,
  start_time timestamp with time zone,
  end_time timestamp with time zone,
  location_name text,
  -- We use the explicit 'extensions.' prefix to be safe
  location_coords extensions.geography(POINT), 
  metadata jsonb default '{}', 
  custom_fields jsonb default '[]',
  sorting_index float8,
  created_at timestamp with time zone default now()
);

create table document_vault (
  id uuid default uuid_generate_v4() primary key,
  trip_id uuid references trips(id) on delete cascade,
  user_id uuid references profiles(id),
  file_name text not null,
  storage_path text not null, -- Path in Supabase Storage
  doc_type text, -- 'passport', 'visa', 'ticket'
  is_encrypted boolean default true,
  created_at timestamp with time zone default now()
);

-- 1. Enable the pgvector extension to work with embeddings
create extension if not exists "vector" with schema "extensions";

-- 2. Enable uuid-ossp for uuid_generate_v4() support
create extension if not exists "uuid-ossp" with schema "extensions";

-- 3. Set the search path so Postgres can find the vector type automatically
set search_path = public, extensions;

create table discovery_embeddings (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references profiles(id),
  content text,
  embedding extensions.vector(1536), -- Change 'vector' to 'extensions.vector'
  location_tag text,
  is_public boolean default false
);

-- This function checks if a user belongs to a specific trip
create or replace function public.is_trip_member(trip_id uuid, req_role text default 'viewer')
returns boolean 
language plpgsql 
security definer 
-- FIX: Set a secure search_path
set search_path = public, auth
as $$
begin
  return exists (
    select 1 from public.trip_members -- Use 'public.' prefix
    where public.trip_members.trip_id = is_trip_member.trip_id
    and public.trip_members.user_id = auth.uid()
    and (
      req_role = 'viewer' 
      or (req_role = 'editor' and role in ('owner', 'editor'))
      or (req_role = 'owner' and role = 'owner')
    )
  );
end;
$$;

alter table profiles enable row level security;

create policy "Profiles are viewable by everyone" 
on profiles for select using (true);

drop policy if exists "Users can update own profile" on profiles;
create policy "Users can update own profile" 
on profiles for update using ( (select auth.uid()) = id );

alter table trips enable row level security;

create policy "Members or template seekers can view trips"
on trips for select using (
  is_trip_member(id, 'viewer') or is_template = true
);

create policy "Owners and editors can update trips"
on trips for update using (is_trip_member(id, 'editor'));

alter table itinerary_items enable row level security;

-- 3. Optimized & Consolidated Itinerary Items (Fixes Multiple Permissive)
drop policy if exists "Trip members can view itinerary" on itinerary_items;
drop policy if exists "Editors can insert itinerary items" on itinerary_items;
drop policy if exists "Editors can update/delete itinerary items" on itinerary_items;

-- Single SELECT policy for everyone in the trip (Viewers + Editors)
create policy "itinerary_select_policy"
on itinerary_items for select 
using ( (select public.is_trip_member(trip_id, 'viewer')) );

-- Single INSERT policy for Editors/Owners
create policy "itinerary_insert_policy"
on itinerary_items for insert 
with check ( (select public.is_trip_member(trip_id, 'editor')) );

-- Single UPDATE policy for Editors/Owners
create policy "itinerary_update_policy"
on itinerary_items for update 180386
using ( (select public.is_trip_member(trip_id, 'editor')) );

-- Single DELETE policy for Editors/Owners
create policy "itinerary_delete_policy"
on itinerary_items for delete 
using ( (select public.is_trip_member(trip_id, 'editor')) );



-- 2. Optimized Document Vault Policies
drop policy if exists "Users can only see their own documents" on document_vault;
drop policy if exists "Users can only upload their own documents" on document_vault;

create policy "Users can only see their own documents"
on document_vault for select using ( (select auth.uid()) = user_id );

create policy "Users can only upload their own documents"
on document_vault for insert with check ( (select auth.uid()) = user_id );


-- Enable RLS
alter table public.discovery_embeddings enable row level security;
alter table public.trip_members enable row level security;

-- Drop the old policy (replace the name if yours is slightly different)
drop policy if exists  "Users can view their own memberships" on public.trip_members;

-- Recreate with the performance fix
create policy "Users can view their own memberships"
on public.trip_members
for select
using (
  user_id = (select auth.uid())
);

-- Drop the old policy
drop policy if exists  "Users can view their own embeddings" on public.discovery_embeddings;

-- Recreate with the performance fix
create policy "Users can view their own embeddings"
on public.discovery_embeddings
for select
using (
  user_id = (select auth.uid())
);