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
-- 2. PENGATURAN REKENING DONASI & KUOTA PROMO GRATIS (SELAMANYA / 1 TAHUN)
-- ============================================================================
create table if not exists public.developer_settings (
  id text primary key,
  bank_name text not null default '',
  account_number text not null default '',
  account_holder text not null default '',
  whatsapp_number text not null default '',
  support_email text not null default 'egisbahricha@gmail.com',
  notes text not null default 'Cantumkan email akun KASLITE atau nama toko pada berita transfer.',
  free_lifetime_quota integer not null default 0 check (free_lifetime_quota >= 0),
  free_yearly_quota integer not null default 0 check (free_yearly_quota >= 0),
  updated_at timestamptz not null default now()
);

alter table public.developer_settings
  add column if not exists free_lifetime_quota integer not null default 0;

alter table public.developer_settings
  add column if not exists free_yearly_quota integer not null default 0;

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
-- 2B. PROMO GRATIS SELAMANYA & GRATIS 1 TAHUN UNTUK N PENDAFTAR PERTAMA
-- ============================================================================
create or replace function public.kaslite_get_promo_status()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_lifetime_quota integer := 0;
  v_yearly_quota integer := 0;
  v_used integer := 0;
  v_lifetime_remaining integer := 0;
  v_yearly_remaining integer := 0;
begin
  select coalesce(free_lifetime_quota, 0),
         coalesce(free_yearly_quota, 0)
    into v_lifetime_quota, v_yearly_quota
    from public.developer_settings
   where id = 'payment_config'
   limit 1;

  v_lifetime_quota := greatest(0, coalesce(v_lifetime_quota, 0));
  v_yearly_quota := greatest(0, coalesce(v_yearly_quota, 0));

  select count(*)::integer
    into v_used
    from public.licenses
   where lower(coalesce(owner_email, '')) <> 'egisbahricha@gmail.com';

  v_lifetime_remaining := greatest(0, v_lifetime_quota - v_used);
  v_yearly_remaining := greatest(0, (v_lifetime_quota + v_yearly_quota) - greatest(v_used, v_lifetime_quota));

  return jsonb_build_object(
    'quota', v_lifetime_quota,
    'used', v_used,
    'remaining', v_lifetime_remaining,
    'yearly_quota', v_yearly_quota,
    'yearly_remaining', v_yearly_remaining
  );
end;
$$;

revoke all on function public.kaslite_get_promo_status() from public;
grant execute on function public.kaslite_get_promo_status() to anon, authenticated;

create or replace function public.kaslite_register_customer_license(
  p_license_key text,
  p_store_name text,
  p_owner_name text,
  p_phone text,
  p_device_id text
)
returns setof public.licenses
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid text := coalesce(auth.uid()::text, '');
  v_email text := lower(trim(coalesce(auth.jwt() ->> 'email', '')));
  v_existing public.licenses%rowtype;
  v_inserted public.licenses%rowtype;
  v_lifetime_quota integer := 0;
  v_yearly_quota integer := 0;
  v_used integer := 0;
  v_now_millis bigint := (extract(epoch from now()) * 1000)::bigint;
  v_demo_end_millis bigint := ((extract(epoch from now()) * 1000)::bigint) + 259200000;
  v_yearly_end_millis bigint := ((extract(epoch from now()) * 1000)::bigint) + 31536000000;
  v_promo_tier text := 'NONE';
  v_clean_key text := nullif(trim(coalesce(p_license_key, '')), '');
begin
  if v_uid = '' or v_email = '' then
    raise exception 'Sesi login Supabase diperlukan untuk registrasi lisensi.';
  end if;

  perform pg_advisory_xact_lock(hashtext('kaslite_free_lifetime_promo_lock'));

  -- Cek apakah user sudah punya lisensi atas UID atau DEMO yang belum diklaim dengan email sama
  select *
    into v_existing
    from public.licenses
   where owner_uid = v_uid
      or (lower(owner_email) = v_email and (coalesce(owner_uid, '') = '' and plan_type = 'DEMO'))
   order by
     case when plan_type = 'FULL' or status = 'ACTIVE' then 0 else 1 end,
     updated_at desc,
     created_at desc
   limit 1;

  if found then
    if coalesce(v_existing.device_id, '') <> ''
       and coalesce(trim(p_device_id), '') <> ''
       and v_existing.device_id <> trim(p_device_id) then
      return next v_existing;
      return;
    end if;

    if coalesce(v_existing.owner_uid, '') = '' or coalesce(v_existing.device_id, '') = '' then
      update public.licenses
         set owner_uid = case when coalesce(owner_uid, '') = '' then v_uid else owner_uid end,
             owner_email = case when coalesce(owner_email, '') = '' then v_email else owner_email end,
             owner_name = case when coalesce(owner_name, '') = '' and trim(coalesce(p_owner_name, '')) <> '' then trim(p_owner_name) else owner_name end,
             store_name = case when coalesce(store_name, '') = '' and trim(coalesce(p_store_name, '')) <> '' then trim(p_store_name) else store_name end,
             phone = case when coalesce(phone, '') = '' and trim(coalesce(p_phone, '')) <> '' then trim(p_phone) else phone end,
             device_id = case when coalesce(device_id, '') = '' and trim(coalesce(p_device_id, '')) <> '' then trim(p_device_id) else device_id end,
             updated_at = now()
       where id = v_existing.id
       returning * into v_existing;
    end if;
    return next v_existing;
    return;
  end if;

  if v_clean_key is null then
    v_clean_key := 'KASLITE-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
  end if;

  select coalesce(free_lifetime_quota, 0),
         coalesce(free_yearly_quota, 0)
    into v_lifetime_quota, v_yearly_quota
    from public.developer_settings
   where id = 'payment_config'
   limit 1;

  v_lifetime_quota := greatest(0, coalesce(v_lifetime_quota, 0));
  v_yearly_quota := greatest(0, coalesce(v_yearly_quota, 0));

  if (v_lifetime_quota > 0 or v_yearly_quota > 0) and v_email <> 'egisbahricha@gmail.com' then
    select count(*)::integer
      into v_used
      from public.licenses
     where lower(coalesce(owner_email, '')) <> 'egisbahricha@gmail.com';
    if v_lifetime_quota > 0 and v_used < v_lifetime_quota then
      v_promo_tier := 'LIFETIME';
    elsif v_yearly_quota > 0 and v_used < (v_lifetime_quota + v_yearly_quota) then
      v_promo_tier := 'YEARLY';
    end if;
  end if;

  insert into public.licenses (
    license_key,
    store_name,
    owner_name,
    owner_email,
    owner_uid,
    phone,
    device_id,
    plan_type,
    status,
    demo_started_at_millis,
    demo_expires_at_millis,
    activated_at_millis,
    expires_at_millis
  ) values (
    v_clean_key,
    trim(coalesce(p_store_name, '')),
    trim(coalesce(p_owner_name, '')),
    v_email,
    v_uid,
    trim(coalesce(p_phone, '')),
    trim(coalesce(p_device_id, '')),
    case when v_promo_tier <> 'NONE' then 'FULL' else 'DEMO' end,
    case when v_promo_tier <> 'NONE' then 'ACTIVE' else 'DEMO' end,
    case when v_promo_tier <> 'NONE' then 0 else v_now_millis end,
    case when v_promo_tier <> 'NONE' then 0 else v_demo_end_millis end,
    case when v_promo_tier <> 'NONE' then v_now_millis else 0 end,
    case when v_promo_tier = 'YEARLY' then v_yearly_end_millis else 0 end
  )
  returning * into v_inserted;

  return next v_inserted;
  return;
end;
$$;

revoke all on function public.kaslite_register_customer_license(text, text, text, text, text) from public, anon;
grant execute on function public.kaslite_register_customer_license(text, text, text, text, text) to authenticated;

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
