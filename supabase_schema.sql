-- Jalankan SQL ini satu kali di Supabase Dashboard -> SQL Editor -> New Query
-- Tabel lisensi & data pelanggan untuk Super Admin KASLITE (egisbahricha@gmail.com)

create table if not exists public.licenses (
  id uuid primary key default gen_random_uuid(),
  license_key text not null unique,
  store_name text not null default '',
  owner_name text not null default '',
  owner_email text not null,
  owner_uid text not null default '',
  phone text not null default '',
  device_id text not null default '',
  plan_type text not null default 'DEMO' check (plan_type in ('DEMO', 'FULL')),
  status text not null default 'DEMO' check (status in ('DEMO', 'ACTIVE', 'EXPIRED', 'SUSPENDED')),
  demo_started_at_millis bigint not null default 0,
  demo_expires_at_millis bigint not null default 0,
  activated_at_millis bigint not null default 0,
  expires_at_millis bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_licenses_owner_uid on public.licenses (owner_uid);
create index if not exists idx_licenses_owner_email on public.licenses (lower(owner_email));

create or replace function public.kaslite_is_super_admin()
returns boolean
language sql
stable
as $$
  select lower(coalesce(auth.jwt() ->> 'email', '')) = 'egisbahricha@gmail.com';
$$;

create or replace function public.kaslite_guard_license_update()
returns trigger
language plpgsql
security definer
as $$
begin
  new.updated_at := now();
  if not public.kaslite_is_super_admin() then
    -- Pelanggan tidak boleh memperpanjang/mengubah paket atau mengambil alih perangkat lain sendiri
    new.plan_type := old.plan_type;
    new.status := old.status;
    new.demo_started_at_millis := old.demo_started_at_millis;
    new.demo_expires_at_millis := old.demo_expires_at_millis;
    new.activated_at_millis := old.activated_at_millis;
    new.expires_at_millis := old.expires_at_millis;
    if coalesce(old.device_id, '') <> '' and coalesce(new.device_id, '') <> old.device_id then
      raise exception 'Perangkat terikat lisensi lain. Hubungi Admin Pusat untuk Reset Device.';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_kaslite_guard_license_update on public.licenses;
create trigger trg_kaslite_guard_license_update
  before update on public.licenses
  for each row
  execute function public.kaslite_guard_license_update();

alter table public.licenses enable row level security;

drop policy if exists "licenses_select_owner_or_admin" on public.licenses;
create policy "licenses_select_owner_or_admin"
  on public.licenses
  for select
  to authenticated
  using (
    public.kaslite_is_super_admin()
    or owner_uid = auth.uid()::text
    or lower(owner_email) = lower(coalesce(auth.jwt() ->> 'email', ''))
    or coalesce(owner_uid, '') = ''
  );

drop policy if exists "licenses_insert_self_demo_or_admin" on public.licenses;
create policy "licenses_insert_self_demo_or_admin"
  on public.licenses
  for insert
  to authenticated
  with check (
    public.kaslite_is_super_admin()
    or (
      owner_uid = auth.uid()::text
      and lower(owner_email) = lower(coalesce(auth.jwt() ->> 'email', ''))
      and plan_type = 'DEMO'
      and status = 'DEMO'
    )
  );

drop policy if exists "licenses_update_owner_claim_or_admin" on public.licenses;
create policy "licenses_update_owner_claim_or_admin"
  on public.licenses
  for update
  to authenticated
  using (
    public.kaslite_is_super_admin()
    or owner_uid = auth.uid()::text
    or lower(owner_email) = lower(coalesce(auth.jwt() ->> 'email', ''))
    or coalesce(owner_uid, '') = ''
  )
  with check (
    public.kaslite_is_super_admin()
    or (
      owner_uid = auth.uid()::text
      and lower(owner_email) = lower(coalesce(auth.jwt() ->> 'email', ''))
    )
  );

drop policy if exists "licenses_delete_admin_only" on public.licenses;
create policy "licenses_delete_admin_only"
  on public.licenses
  for delete
  to authenticated
  using (public.kaslite_is_super_admin());
