-- Jalankan SQL ini satu kali di Supabase Dashboard -> SQL Editor -> New Query
-- Tabel lisensi, pengaturan rekening donasi, konfirmasi donasi & laporan bug KASLITE (egisbahricha@gmail.com)

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

-- ============================================================================
-- 2. PENGATURAN REKENING DONASI PENGEMBANG (Bisa diubah langsung di Super Admin)
-- ============================================================================
create table if not exists public.developer_settings (
  id text primary key,
  bank_name text not null default '',
  account_number text not null default '',
  account_holder text not null default '',
  whatsapp_number text not null default '',
  support_email text not null default 'egisbahricha@gmail.com',
  notes text not null default 'Cantumkan email akun KASLITE atau nama toko pada berita transfer.',
  updated_at timestamptz not null default now()
);

alter table public.developer_settings enable row level security;

drop policy if exists "dev_settings_select_public" on public.developer_settings;
create policy "dev_settings_select_public"
  on public.developer_settings
  for select
  to anon, authenticated
  using (true);

drop policy if exists "dev_settings_write_admin_only" on public.developer_settings;
create policy "dev_settings_write_admin_only"
  on public.developer_settings
  for all
  to authenticated
  using (public.kaslite_is_super_admin())
  with check (public.kaslite_is_super_admin());

-- ============================================================================
-- 3. KONFIRMASI DONASI KE PENGEMBANG (Manual Transfer -> Verifikasi Super Admin)
-- ============================================================================
create table if not exists public.donation_requests (
  id uuid primary key default gen_random_uuid(),
  user_uid text not null default '',
  user_email text not null,
  owner_name text not null default '',
  store_name text not null default '',
  phone text not null default '',
  device_id text not null default '',
  package_id text not null check (package_id in ('MONTHLY', 'YEARLY', 'LIFETIME')),
  package_title text not null default '',
  amount_rupiah bigint not null default 0,
  duration_days integer not null default 30,
  sender_bank_name text not null default '',
  sender_account_name text not null default '',
  transfer_note text not null default '',
  status text not null default 'PENDING' check (status in ('PENDING', 'PAID', 'REJECTED')),
  license_key text not null default '',
  admin_notes text not null default '',
  approved_at_millis bigint not null default 0,
  email_sent_at_millis bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_donations_user_uid on public.donation_requests (user_uid);
create index if not exists idx_donations_status on public.donation_requests (status);

alter table public.donation_requests enable row level security;

drop policy if exists "donations_select_owner_or_admin" on public.donation_requests;
create policy "donations_select_owner_or_admin"
  on public.donation_requests
  for select
  to authenticated
  using (
    public.kaslite_is_super_admin()
    or user_uid = auth.uid()::text
    or lower(user_email) = lower(coalesce(auth.jwt() ->> 'email', ''))
  );

drop policy if exists "donations_insert_customer" on public.donation_requests;
create policy "donations_insert_customer"
  on public.donation_requests
  for insert
  to authenticated
  with check (
    user_uid = auth.uid()::text
    and status = 'PENDING'
  );

drop policy if exists "donations_update_admin_only" on public.donation_requests;
create policy "donations_update_admin_only"
  on public.donation_requests
  for update
  to authenticated
  using (public.kaslite_is_super_admin())
  with check (public.kaslite_is_super_admin());

drop policy if exists "donations_delete_admin_only" on public.donation_requests;
create policy "donations_delete_admin_only"
  on public.donation_requests
  for delete
  to authenticated
  using (public.kaslite_is_super_admin());

-- ============================================================================
-- 4. LAPORAN BUG PELANGGAN
-- ============================================================================
create table if not exists public.bug_reports (
  id uuid primary key default gen_random_uuid(),
  user_uid text not null default '',
  user_email text not null default '',
  reporter_name text not null default '',
  store_name text not null default '',
  device_id text not null default '',
  category text not null default 'Kasir POS',
  severity text not null default 'SEDANG' check (severity in ('RENDAH', 'SEDANG', 'TINGGI', 'KRITIS')),
  title text not null,
  description text not null,
  app_version text not null default '',
  device_model text not null default '',
  android_version text not null default '',
  status text not null default 'BARU' check (status in ('BARU', 'DIPROSES', 'SELESAI', 'DITOLAK')),
  admin_notes text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_bug_reports_status on public.bug_reports (status);
create index if not exists idx_bug_reports_user_uid on public.bug_reports (user_uid);

alter table public.bug_reports enable row level security;

drop policy if exists "bugs_select_owner_or_admin" on public.bug_reports;
create policy "bugs_select_owner_or_admin"
  on public.bug_reports
  for select
  to authenticated
  using (
    public.kaslite_is_super_admin()
    or user_uid = auth.uid()::text
    or lower(user_email) = lower(coalesce(auth.jwt() ->> 'email', ''))
  );

drop policy if exists "bugs_insert_authenticated" on public.bug_reports;
create policy "bugs_insert_authenticated"
  on public.bug_reports
  for insert
  to authenticated
  with check (
    user_uid = auth.uid()::text
    and status = 'BARU'
  );

drop policy if exists "bugs_update_admin_only" on public.bug_reports;
create policy "bugs_update_admin_only"
  on public.bug_reports
  for update
  to authenticated
  using (public.kaslite_is_super_admin())
  with check (public.kaslite_is_super_admin());

drop policy if exists "bugs_delete_admin_only" on public.bug_reports;
create policy "bugs_delete_admin_only"
  on public.bug_reports
  for delete
  to authenticated
  using (public.kaslite_is_super_admin());
