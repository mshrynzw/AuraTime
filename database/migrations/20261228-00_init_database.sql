-- =========================================================
-- 000_init.sql (PostgreSQL / RDS)
-- Multi-tenant: 会社=テナント
-- GROUPSに部署/勤務地/コストセンター等を統合
-- SYSTEM_LOGSは外部（CloudWatch等）想定のためDBには作らない
-- =========================================================
--
-- 使用方法:
--   1. データベースを削除して再作成する場合（開発環境のみ）:
--      このスクリプトの先頭部分（データベース削除セクション）のコメントを外してください
--      重要: postgres データベースに接続してから実行してください
--      psql -U postgres -d postgres -f database/migrations/20261228-00_init_database.sql
--
--   2. docker-compose を使用している場合:
--      【PowerShellの場合】
--      Get-Content database/migrations/20261228-00_init_database.sql | docker exec -i auratime-postgres psql -U postgres -d auratime
--
--      【Bash/CMDの場合】
--      docker exec -i auratime-postgres psql -U postgres -d auratime < database/migrations/220261228-00_init_database.sql
-- =========================================================

-- =========================================================
-- データベース削除・再作成（開発環境のみ）
-- =========================================================
-- 重要: このセクションは開発環境でのみ使用してください
--       本番環境では絶対に実行しないでください
--
-- 実行方法（3段階で実行）:
--   1. 既存の接続を強制終了:
--      docker exec -i auratime-postgres psql -U postgres -d postgres -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'auratime' AND pid <> pg_backend_pid();"
--
--   2. データベース削除・再作成:
--      docker exec -i auratime-postgres psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS auratime;"
--      docker exec -i auratime-postgres psql -U postgres -d postgres -c "CREATE DATABASE auratime WITH OWNER = postgres ENCODING = 'UTF8' LC_COLLATE = 'C' LC_CTYPE = 'C' TEMPLATE = template0;"
--
--   3. スキーマ作成（このファイルを実行）:
--      docker cp database/migrations/20261228-00_init_database.sql auratime-postgres:/tmp/init_database.sql
--      docker exec -i auratime-postgres psql -U postgres -d auratime -f /tmp/init_database.sql
--
--   4. 初期データ投入（別ファイルを実行）:
--      docker cp database/migrations/20261228-01_init_data.sql auratime-postgres:/tmp/init_data.sql
--      docker exec -i auratime-postgres psql -U postgres -d auratime -f /tmp/init_data.sql
-- =========================================================

-- =========================================================

-- クライアントエンコーディングをUTF-8に設定
SET client_encoding TO 'UTF8';

-- UUID生成（pgcrypto）
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- UUID v7生成関数（RFC 4122準拠、タイムスタンプベースでソート可能）
CREATE OR REPLACE FUNCTION gen_uuid_v7()
RETURNS uuid AS $$
DECLARE
  unix_ts_ms bigint;
  uuid_bytes bytea;
BEGIN
  -- Unixタイムスタンプ（ミリ秒）を取得
  -- EXTRACT(EPOCH FROM now())はnumeric型を返すため、bigintにキャスト
  unix_ts_ms := (EXTRACT(EPOCH FROM now()) * 1000)::bigint;

  -- UUID v7フォーマット: タイムスタンプ(48bit) + バージョン(4bit) + ランダム(12bit) + バリアント(2bit) + ランダム(62bit)
  -- タイムスタンプを48bitに変換（上位16bitは0埋め）
  uuid_bytes :=
    set_byte(
      set_byte(
        set_byte(
          set_byte(
            set_byte(
              set_byte(
                gen_random_bytes(16),
                0, ((unix_ts_ms >> 40) & 255)::int
              ),
              1, ((unix_ts_ms >> 32) & 255)::int
            ),
            2, ((unix_ts_ms >> 24) & 255)::int
          ),
          3, ((unix_ts_ms >> 16) & 255)::int
        ),
        4, ((unix_ts_ms >> 8) & 255)::int
      ),
      5, (unix_ts_ms & 255)::int
    );

  -- バージョン7を設定（7番目のバイトの上位4bitを0x70に設定）
  uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & 15) | 112);

  -- バリアント（8番目のバイトの上位2bitを10に設定）
  uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & 63) | 128);

  RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION gen_uuid_v7() IS 'UUID v7を生成（タイムスタンプベース、時系列順にソート可能）';

-- updated_at 自動更新（updated_by はアプリで必ず設定）
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- グループタイプチェック関数（CHECK制約用）
CREATE OR REPLACE FUNCTION check_group_type(group_id uuid, expected_type text)
RETURNS boolean AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM m_groups
    WHERE id = group_id
      AND type = expected_type
      AND deleted_at IS NULL
  );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION check_group_type(uuid, text) IS '指定されたgroup_idが期待するtypeかチェック（CHECK制約用）';

-- =========================================================
-- 1) マスタ系 (m_)
-- =========================================================

-- m_companies（会社=テナント）
CREATE TABLE m_companies (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  name text NOT NULL,
  code text NOT NULL,
  timezone text NOT NULL DEFAULT 'Asia/Tokyo',
  currency text NOT NULL DEFAULT 'JPY',

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL,
  deleted_at timestamptz,
  deleted_by uuid
);

COMMENT ON TABLE m_companies IS '会社（テナント）';

COMMENT ON COLUMN m_companies.id IS '会社ID';
COMMENT ON COLUMN m_companies.name IS '会社名';
COMMENT ON COLUMN m_companies.code IS '会社コード';
COMMENT ON COLUMN m_companies.timezone IS 'タイムゾーン';
COMMENT ON COLUMN m_companies.currency IS '通貨';

COMMENT ON COLUMN m_companies.created_at IS '作成日時';
COMMENT ON COLUMN m_companies.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_companies.updated_at IS '更新日時';
COMMENT ON COLUMN m_companies.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_companies.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_companies.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_m_companies_code
  ON m_companies(code)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_companies
BEFORE UPDATE ON m_companies
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- m_users（ユーザー）
CREATE TABLE m_users (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  email text NOT NULL,
  password_hash text NOT NULL,
  family_name text NOT NULL,
  first_name text NOT NULL,
  family_name_kana text,
  first_name_kana text,
  status text NOT NULL DEFAULT 'active', -- active|inactive|locked など

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL,
  deleted_at timestamptz,
  deleted_by uuid
);

COMMENT ON TABLE m_users IS 'ユーザー';

COMMENT ON COLUMN m_users.id IS 'ユーザーID';
COMMENT ON COLUMN m_users.email IS 'メールアドレス';
COMMENT ON COLUMN m_users.password_hash IS 'パスワードハッシュ（bcrypt等）';
COMMENT ON COLUMN m_users.family_name IS '姓';
COMMENT ON COLUMN m_users.first_name IS '名';
COMMENT ON COLUMN m_users.family_name_kana IS '姓（カナ）';
COMMENT ON COLUMN m_users.first_name_kana IS '名（カナ）';
COMMENT ON COLUMN m_users.status IS '状態（active|inactive|locked）';

COMMENT ON COLUMN m_users.created_at IS '作成日時';
COMMENT ON COLUMN m_users.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_users.updated_at IS '更新日時';
COMMENT ON COLUMN m_users.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_users.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_users.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_m_users_email
  ON m_users(email)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_users
BEFORE UPDATE ON m_users
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- FK（循環を避けつつ NOT NULL を維持するため DEFERRABLE）
ALTER TABLE m_companies
  ADD CONSTRAINT fk_m_companies_created_by
  FOREIGN KEY (created_by) REFERENCES m_users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE m_companies
  ADD CONSTRAINT fk_m_companies_updated_by
  FOREIGN KEY (updated_by) REFERENCES m_users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE m_companies
  ADD CONSTRAINT fk_m_companies_deleted_by
  FOREIGN KEY (deleted_by) REFERENCES m_users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE m_users
  ADD CONSTRAINT fk_m_users_created_by
  FOREIGN KEY (created_by) REFERENCES m_users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE m_users
  ADD CONSTRAINT fk_m_users_updated_by
  FOREIGN KEY (updated_by) REFERENCES m_users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE m_users
  ADD CONSTRAINT fk_m_users_deleted_by
  FOREIGN KEY (deleted_by) REFERENCES m_users(id)
  DEFERRABLE INITIALLY DEFERRED;


-- r_company_memberships（会社所属/権限）
CREATE TABLE r_company_memberships (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES m_companies(id),
  user_id uuid NOT NULL REFERENCES m_users(id),
  role text NOT NULL, -- system_admin|admin|manager|employee
  joined_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE r_company_memberships IS '会社所属（権限）';
COMMENT ON COLUMN r_company_memberships.company_id IS '会社ID';
COMMENT ON COLUMN r_company_memberships.user_id IS 'ユーザーID';
COMMENT ON COLUMN r_company_memberships.role IS '権限ロール（system_admin|admin|manager|employee）';
COMMENT ON COLUMN r_company_memberships.joined_at IS '参加日時';

COMMENT ON COLUMN r_company_memberships.created_at IS '作成日時';
COMMENT ON COLUMN r_company_memberships.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN r_company_memberships.updated_at IS '更新日時';
COMMENT ON COLUMN r_company_memberships.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN r_company_memberships.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN r_company_memberships.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_r_company_memberships_company_user
  ON r_company_memberships(company_id, user_id)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_r_company_memberships_company
  ON r_company_memberships(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_r_company_memberships
BEFORE UPDATE ON r_company_memberships
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- m_groups（部署/勤務地/チーム/コストセンター等の統合）
CREATE TABLE m_groups (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES m_companies(id),
  type text NOT NULL, -- department|work_location|team|cost_center|custom
  name text NOT NULL,
  code text,
  parent_group_id uuid REFERENCES m_groups(id),
  sort_order int NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE m_groups IS 'グループ（部署/勤務地/チーム/コストセンター等）';
COMMENT ON COLUMN m_groups.company_id IS '会社ID';
COMMENT ON COLUMN m_groups.type IS 'グループ種別';
COMMENT ON COLUMN m_groups.name IS 'グループ名';
COMMENT ON COLUMN m_groups.code IS 'グループコード';
COMMENT ON COLUMN m_groups.parent_group_id IS '親グループID';
COMMENT ON COLUMN m_groups.sort_order IS '表示順';

COMMENT ON COLUMN m_groups.created_at IS '作成日時';
COMMENT ON COLUMN m_groups.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_groups.updated_at IS '更新日時';
COMMENT ON COLUMN m_groups.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_groups.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_groups.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_m_groups_company_type
  ON m_groups(company_id, type)
  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_m_groups_company_type_code
  ON m_groups(company_id, type, code)
  WHERE deleted_at IS NULL AND code IS NOT NULL;

CREATE TRIGGER set_updated_at_m_groups
BEFORE UPDATE ON m_groups
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- m_employees（従業員）
CREATE TABLE m_employees (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES m_companies(id),
  user_id uuid REFERENCES m_users(id),
  employee_no text NOT NULL,
  employment_type text NOT NULL, -- fulltime|parttime|contract
  hire_date date NOT NULL,
  termination_date date,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE m_employees IS '従業員';
COMMENT ON COLUMN m_employees.company_id IS '会社ID';
COMMENT ON COLUMN m_employees.user_id IS 'ユーザーID（ログイン紐付け）';
COMMENT ON COLUMN m_employees.employee_no IS '社員番号';
COMMENT ON COLUMN m_employees.employment_type IS '雇用区分';
COMMENT ON COLUMN m_employees.hire_date IS '入社日';
COMMENT ON COLUMN m_employees.termination_date IS '退職日';

COMMENT ON COLUMN m_employees.created_at IS '作成日時';
COMMENT ON COLUMN m_employees.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_employees.updated_at IS '更新日時';
COMMENT ON COLUMN m_employees.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_employees.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_employees.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_m_employees_company_employee_no
  ON m_employees(company_id, employee_no)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_m_employees_company
  ON m_employees(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_employees
BEFORE UPDATE ON m_employees
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- h_employee_groups（所属：履歴/兼務/役割）
CREATE TABLE h_employee_groups (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES m_companies(id),
  employee_id uuid NOT NULL REFERENCES m_employees(id),
  group_id uuid NOT NULL REFERENCES m_groups(id),

  role_in_group text NOT NULL DEFAULT 'member', -- member|leader
  valid_from date NOT NULL,
  valid_to date,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE h_employee_groups IS '従業員のグループ所属（履歴・兼務対応）';
COMMENT ON COLUMN h_employee_groups.company_id IS '会社ID';
COMMENT ON COLUMN h_employee_groups.employee_id IS '従業員ID';
COMMENT ON COLUMN h_employee_groups.group_id IS 'グループID';
COMMENT ON COLUMN h_employee_groups.role_in_group IS 'グループ内役割（leader|member）';
COMMENT ON COLUMN h_employee_groups.valid_from IS '有効開始日';
COMMENT ON COLUMN h_employee_groups.valid_to IS '有効終了日';

COMMENT ON COLUMN h_employee_groups.created_at IS '作成日時';
COMMENT ON COLUMN h_employee_groups.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN h_employee_groups.updated_at IS '更新日時';
COMMENT ON COLUMN h_employee_groups.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN h_employee_groups.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN h_employee_groups.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_h_employee_groups_company_employee
  ON h_employee_groups(company_id, employee_id)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_h_employee_groups_company_group
  ON h_employee_groups(company_id, group_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_h_employee_groups
BEFORE UPDATE ON h_employee_groups
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 2) 勤怠
-- =========================================================

-- m_shift_templates（シフトテンプレ）
CREATE TABLE m_shift_templates (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  name text NOT NULL,
  default_start time,
  default_end time,
  break_minutes int NOT NULL DEFAULT 0,
  is_night_shift boolean NOT NULL DEFAULT false,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE m_shift_templates IS 'シフトテンプレート';
COMMENT ON COLUMN m_shift_templates.company_id IS '会社ID';
COMMENT ON COLUMN m_shift_templates.name IS '名称';
COMMENT ON COLUMN m_shift_templates.default_start IS '開始時刻（規定）';
COMMENT ON COLUMN m_shift_templates.default_end IS '終了時刻（規定）';
COMMENT ON COLUMN m_shift_templates.break_minutes IS '休憩分（規定）';
COMMENT ON COLUMN m_shift_templates.is_night_shift IS '夜勤フラグ';

COMMENT ON COLUMN m_shift_templates.created_at IS '作成日時';
COMMENT ON COLUMN m_shift_templates.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_shift_templates.updated_at IS '更新日時';
COMMENT ON COLUMN m_shift_templates.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_shift_templates.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_shift_templates.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_m_shift_templates_company
  ON m_shift_templates(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_shift_templates
BEFORE UPDATE ON m_shift_templates
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- r_shift_break_templates（シフト休憩テンプレ）
CREATE TABLE r_shift_break_templates (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),
  shift_template_id uuid NOT NULL REFERENCES m_shift_templates(id),

  break_start time NOT NULL,
  break_end time NOT NULL,
  break_name text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,

  CONSTRAINT ck_break_time_range CHECK (break_start < break_end)
);

COMMENT ON TABLE r_shift_break_templates IS 'シフト休憩テンプレート';
COMMENT ON COLUMN r_shift_break_templates.company_id IS '会社ID';
COMMENT ON COLUMN r_shift_break_templates.shift_template_id IS 'シフトテンプレートID';
COMMENT ON COLUMN r_shift_break_templates.break_start IS '休憩開始時刻';
COMMENT ON COLUMN r_shift_break_templates.break_end IS '休憩終了時刻';
COMMENT ON COLUMN r_shift_break_templates.break_name IS '休憩名';

CREATE INDEX ix_r_shift_break_templates_shift
  ON r_shift_break_templates(shift_template_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_r_shift_break_templates
BEFORE UPDATE ON r_shift_break_templates
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- r_shift_assignments（シフト割当）
CREATE TABLE r_shift_assignments (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  employee_id uuid NOT NULL REFERENCES m_employees(id),
  work_date date NOT NULL,
  shift_template_id uuid REFERENCES m_shift_templates(id),
  group_id uuid REFERENCES m_groups(id), -- GROUPS(type=work_location等)

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE r_shift_assignments IS 'シフト割当';
COMMENT ON COLUMN r_shift_assignments.company_id IS '会社ID';
COMMENT ON COLUMN r_shift_assignments.employee_id IS '従業員ID';
COMMENT ON COLUMN r_shift_assignments.work_date IS '勤務日';
COMMENT ON COLUMN r_shift_assignments.shift_template_id IS 'シフトテンプレートID';
COMMENT ON COLUMN r_shift_assignments.group_id IS '勤務グループID（m_groups.type = work_location のみ許可）';

COMMENT ON COLUMN r_shift_assignments.created_at IS '作成日時';
COMMENT ON COLUMN r_shift_assignments.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN r_shift_assignments.updated_at IS '更新日時';
COMMENT ON COLUMN r_shift_assignments.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN r_shift_assignments.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN r_shift_assignments.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_r_shift_assignments_company_employee_date
  ON r_shift_assignments(company_id, employee_id, work_date)
  WHERE deleted_at IS NULL;

-- CHECK制約: group_idはwork_locationタイプのみ許可
ALTER TABLE r_shift_assignments
  ADD CONSTRAINT chk_shift_group_type
  CHECK (group_id IS NULL OR check_group_type(group_id, 'work_location'));

CREATE TRIGGER set_updated_at_r_shift_assignments
BEFORE UPDATE ON r_shift_assignments
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- h_time_clock_events（打刻：生ログ）
CREATE TABLE h_time_clock_events (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  employee_id uuid NOT NULL REFERENCES m_employees(id),
  happened_at timestamptz NOT NULL,
  type text NOT NULL,   -- in|out|break_in|break_out
  source text NOT NULL, -- web|mobile|ic_card|admin (ic_cardは将来対応)
  note text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE h_time_clock_events IS '打刻イベント（生ログ）';
COMMENT ON COLUMN h_time_clock_events.company_id IS '会社ID';
COMMENT ON COLUMN h_time_clock_events.employee_id IS '従業員ID';
COMMENT ON COLUMN h_time_clock_events.happened_at IS '発生日時';
COMMENT ON COLUMN h_time_clock_events.type IS '打刻種別（in|out|break_in|break_out）';
COMMENT ON COLUMN h_time_clock_events.source IS '入力元（web|mobile|ic_card|admin）。ic_cardは将来対応。';
COMMENT ON COLUMN h_time_clock_events.note IS '備考（遅延理由、位置情報等）';

COMMENT ON COLUMN h_time_clock_events.created_at IS '作成日時';
COMMENT ON COLUMN h_time_clock_events.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN h_time_clock_events.updated_at IS '更新日時';
COMMENT ON COLUMN h_time_clock_events.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN h_time_clock_events.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN h_time_clock_events.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_h_time_clock_events_company_employee_time
  ON h_time_clock_events(company_id, employee_id, happened_at)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_h_time_clock_events
BEFORE UPDATE ON h_time_clock_events
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- t_time_records（勤怠：日次集計/申請承認）
CREATE TABLE t_time_records (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  employee_id uuid NOT NULL REFERENCES m_employees(id),
  work_date date NOT NULL,

  start_at timestamptz,
  end_at timestamptz,
  break_minutes int NOT NULL DEFAULT 0,
  work_minutes int NOT NULL DEFAULT 0,
  overtime_minutes int NOT NULL DEFAULT 0,
  night_minutes int NOT NULL DEFAULT 0,

  status text NOT NULL DEFAULT 'draft', -- draft|submitted|approved|rejected|locked
  submitted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  submitted_at timestamptz,
  approved_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  approved_at timestamptz,

  group_id uuid REFERENCES m_groups(id), -- GROUPS(type=cost_center等)

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_time_records IS '勤怠レコード（集計・申請承認）';
COMMENT ON COLUMN t_time_records.company_id IS '会社ID';
COMMENT ON COLUMN t_time_records.employee_id IS '従業員ID';
COMMENT ON COLUMN t_time_records.work_date IS '勤務日';
COMMENT ON COLUMN t_time_records.start_at IS '開始日時';
COMMENT ON COLUMN t_time_records.end_at IS '終了日時';
COMMENT ON COLUMN t_time_records.break_minutes IS '休憩分';
COMMENT ON COLUMN t_time_records.work_minutes IS '労働分';
COMMENT ON COLUMN t_time_records.overtime_minutes IS '残業分';
COMMENT ON COLUMN t_time_records.night_minutes IS '深夜分';
COMMENT ON COLUMN t_time_records.status IS '状態（draft|submitted|approved|rejected|locked）';
COMMENT ON COLUMN t_time_records.submitted_by IS '申請者ユーザーID';
COMMENT ON COLUMN t_time_records.submitted_at IS '申請日時';
COMMENT ON COLUMN t_time_records.approved_by IS '承認者ユーザーID';
COMMENT ON COLUMN t_time_records.approved_at IS '承認日時';
COMMENT ON COLUMN t_time_records.group_id IS 'コストセンターID（m_groups.type = cost_center のみ許可、配賦先）';

COMMENT ON COLUMN t_time_records.created_at IS '作成日時';
COMMENT ON COLUMN t_time_records.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_time_records.updated_at IS '更新日時';
COMMENT ON COLUMN t_time_records.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_time_records.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_time_records.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_t_time_records_company_employee_date
  ON t_time_records(company_id, employee_id, work_date)
  WHERE deleted_at IS NULL;

-- CHECK制約: group_idはcost_centerタイプのみ許可
ALTER TABLE t_time_records
  ADD CONSTRAINT chk_time_group_type
  CHECK (group_id IS NULL OR check_group_type(group_id, 'cost_center'));

CREATE INDEX ix_t_time_records_company_date
  ON t_time_records(company_id, work_date)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_time_records
BEFORE UPDATE ON t_time_records
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 3) 承認（汎用）
-- =========================================================

CREATE TABLE t_approval_requests (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  target_type text NOT NULL, -- time_record|leave_request|other
  target_id uuid NOT NULL,

  requester_user_id uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  current_approver_user_id uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  status text NOT NULL DEFAULT 'pending', -- pending|approved|rejected|canceled
  requested_at timestamptz NOT NULL,
  resolved_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_approval_requests IS '承認リクエスト';
COMMENT ON COLUMN t_approval_requests.company_id IS '会社ID';
COMMENT ON COLUMN t_approval_requests.target_type IS '対象種別（time_record|leave_request等）';
COMMENT ON COLUMN t_approval_requests.target_id IS '対象ID';
COMMENT ON COLUMN t_approval_requests.requester_user_id IS '申請者ユーザーID';
COMMENT ON COLUMN t_approval_requests.current_approver_user_id IS '現在承認者ユーザーID';
COMMENT ON COLUMN t_approval_requests.status IS '状態（pending|approved|rejected|canceled）';
COMMENT ON COLUMN t_approval_requests.requested_at IS '申請日時';
COMMENT ON COLUMN t_approval_requests.resolved_at IS '確定日時';

COMMENT ON COLUMN t_approval_requests.created_at IS '作成日時';
COMMENT ON COLUMN t_approval_requests.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_approval_requests.updated_at IS '更新日時';
COMMENT ON COLUMN t_approval_requests.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_approval_requests.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_approval_requests.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_t_approval_requests_company_status
  ON t_approval_requests(company_id, status)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_approval_requests
BEFORE UPDATE ON t_approval_requests
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE h_approval_steps (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  approval_request_id uuid NOT NULL REFERENCES t_approval_requests(id),
  step_no int NOT NULL,
  approver_user_id uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,

  status text NOT NULL DEFAULT 'pending', -- pending|approved|rejected|skipped
  acted_at timestamptz,
  comment text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE h_approval_steps IS '承認ステップ（履歴）';
COMMENT ON COLUMN h_approval_steps.company_id IS '会社ID';
COMMENT ON COLUMN h_approval_steps.approval_request_id IS '承認リクエストID';
COMMENT ON COLUMN h_approval_steps.step_no IS 'ステップ番号';
COMMENT ON COLUMN h_approval_steps.approver_user_id IS '承認者ユーザーID';
COMMENT ON COLUMN h_approval_steps.status IS '状態（pending|approved|rejected|skipped）';
COMMENT ON COLUMN h_approval_steps.acted_at IS '操作日時';
COMMENT ON COLUMN h_approval_steps.comment IS 'コメント';

COMMENT ON COLUMN h_approval_steps.created_at IS '作成日時';
COMMENT ON COLUMN h_approval_steps.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN h_approval_steps.updated_at IS '更新日時';
COMMENT ON COLUMN h_approval_steps.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN h_approval_steps.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN h_approval_steps.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_h_approval_steps_request_step
  ON h_approval_steps(approval_request_id, step_no)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_h_approval_steps
BEFORE UPDATE ON h_approval_steps
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 4) 休暇
-- =========================================================

CREATE TABLE m_leave_types (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  name text NOT NULL,
  code text NOT NULL,
  is_paid boolean NOT NULL DEFAULT false,
  requires_approval boolean NOT NULL DEFAULT true,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE m_leave_types IS '休暇種別';
COMMENT ON COLUMN m_leave_types.company_id IS '会社ID';
COMMENT ON COLUMN m_leave_types.name IS '休暇名';
COMMENT ON COLUMN m_leave_types.code IS '休暇コード';
COMMENT ON COLUMN m_leave_types.is_paid IS '有給フラグ';
COMMENT ON COLUMN m_leave_types.requires_approval IS '承認要否';

COMMENT ON COLUMN m_leave_types.created_at IS '作成日時';
COMMENT ON COLUMN m_leave_types.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_leave_types.updated_at IS '更新日時';
COMMENT ON COLUMN m_leave_types.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_leave_types.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_leave_types.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_m_leave_types_company_code
  ON m_leave_types(company_id, code)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_leave_types
BEFORE UPDATE ON m_leave_types
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE t_leave_requests (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  employee_id uuid NOT NULL REFERENCES m_employees(id),
  leave_type_id uuid NOT NULL REFERENCES m_leave_types(id),

  start_date date NOT NULL,
  end_date date NOT NULL,
  days numeric(6,2) NOT NULL,
  status text NOT NULL DEFAULT 'draft', -- draft|submitted|approved|rejected|canceled
  reason text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_leave_requests IS '休暇申請';
COMMENT ON COLUMN t_leave_requests.company_id IS '会社ID';
COMMENT ON COLUMN t_leave_requests.employee_id IS '従業員ID';
COMMENT ON COLUMN t_leave_requests.leave_type_id IS '休暇種別ID';
COMMENT ON COLUMN t_leave_requests.start_date IS '開始日';
COMMENT ON COLUMN t_leave_requests.end_date IS '終了日';
COMMENT ON COLUMN t_leave_requests.days IS '日数';
COMMENT ON COLUMN t_leave_requests.status IS '状態（draft|submitted|approved|rejected|canceled）';
COMMENT ON COLUMN t_leave_requests.reason IS '理由';

COMMENT ON COLUMN t_leave_requests.created_at IS '作成日時';
COMMENT ON COLUMN t_leave_requests.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_leave_requests.updated_at IS '更新日時';
COMMENT ON COLUMN t_leave_requests.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_leave_requests.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_leave_requests.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_t_leave_requests_company_employee_date
  ON t_leave_requests(company_id, employee_id, start_date, end_date)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_leave_requests
BEFORE UPDATE ON t_leave_requests
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 5) 給与
-- =========================================================

CREATE TABLE m_payroll_calendars (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  name text NOT NULL,
  closing_day int NOT NULL, -- 1-31 or 0=EOM
  payday_day int NOT NULL,  -- 1-31 or 0=EOM

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE m_payroll_calendars IS '給与カレンダー';
COMMENT ON COLUMN m_payroll_calendars.company_id IS '会社ID';
COMMENT ON COLUMN m_payroll_calendars.name IS '名称';
COMMENT ON COLUMN m_payroll_calendars.closing_day IS '締日';
COMMENT ON COLUMN m_payroll_calendars.payday_day IS '支払日';

COMMENT ON COLUMN m_payroll_calendars.created_at IS '作成日時';
COMMENT ON COLUMN m_payroll_calendars.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_payroll_calendars.updated_at IS '更新日時';
COMMENT ON COLUMN m_payroll_calendars.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_payroll_calendars.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_payroll_calendars.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_m_payroll_calendars_company
  ON m_payroll_calendars(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_payroll_calendars
BEFORE UPDATE ON m_payroll_calendars
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE t_payroll_periods (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  payroll_calendar_id uuid NOT NULL REFERENCES m_payroll_calendars(id),
  period_start date NOT NULL,
  period_end date NOT NULL,
  closing_date date NOT NULL,
  pay_date date NOT NULL,
  status text NOT NULL DEFAULT 'open', -- open|closed|calculated|finalized

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_payroll_periods IS '給与期間（締め単位）';
COMMENT ON COLUMN t_payroll_periods.company_id IS '会社ID';
COMMENT ON COLUMN t_payroll_periods.payroll_calendar_id IS '給与カレンダーID';
COMMENT ON COLUMN t_payroll_periods.period_start IS '期間開始日';
COMMENT ON COLUMN t_payroll_periods.period_end IS '期間終了日';
COMMENT ON COLUMN t_payroll_periods.closing_date IS '締日（実日）';
COMMENT ON COLUMN t_payroll_periods.pay_date IS '支払日（実日）';
COMMENT ON COLUMN t_payroll_periods.status IS '状態（open|closed|calculated|finalized）';

COMMENT ON COLUMN t_payroll_periods.created_at IS '作成日時';
COMMENT ON COLUMN t_payroll_periods.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_payroll_periods.updated_at IS '更新日時';
COMMENT ON COLUMN t_payroll_periods.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_payroll_periods.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_payroll_periods.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_t_payroll_periods_company_calendar_range
  ON t_payroll_periods(company_id, payroll_calendar_id, period_start, period_end)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_payroll_periods
BEFORE UPDATE ON t_payroll_periods
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE h_compensation_plans (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  employee_id uuid NOT NULL REFERENCES m_employees(id),
  pay_type text NOT NULL, -- monthly|hourly
  base_salary numeric(12,2),
  hourly_rate numeric(12,2),
  overtime_rate numeric(12,2),
  night_rate numeric(12,2),
  effective_from date NOT NULL,
  effective_to date,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE h_compensation_plans IS '報酬条件（履歴）';
COMMENT ON COLUMN h_compensation_plans.company_id IS '会社ID';
COMMENT ON COLUMN h_compensation_plans.employee_id IS '従業員ID';
COMMENT ON COLUMN h_compensation_plans.pay_type IS '給与形態';
COMMENT ON COLUMN h_compensation_plans.base_salary IS '基本給（月給）';
COMMENT ON COLUMN h_compensation_plans.hourly_rate IS '時給';
COMMENT ON COLUMN h_compensation_plans.overtime_rate IS '残業単価（係数/金額）';
COMMENT ON COLUMN h_compensation_plans.night_rate IS '深夜単価（係数/金額）';
COMMENT ON COLUMN h_compensation_plans.effective_from IS '適用開始日';
COMMENT ON COLUMN h_compensation_plans.effective_to IS '適用終了日';

COMMENT ON COLUMN h_compensation_plans.created_at IS '作成日時';
COMMENT ON COLUMN h_compensation_plans.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN h_compensation_plans.updated_at IS '更新日時';
COMMENT ON COLUMN h_compensation_plans.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN h_compensation_plans.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN h_compensation_plans.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_h_compensation_plans_company_employee_effective
  ON h_compensation_plans(company_id, employee_id, effective_from)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_h_compensation_plans
BEFORE UPDATE ON h_compensation_plans
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE m_pay_items (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  code text NOT NULL,
  name text NOT NULL,
  type text NOT NULL, -- earning|deduction|employer_cost
  taxable boolean NOT NULL DEFAULT true,
  sort_order int NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE m_pay_items IS '給与項目（支給/控除など）';
COMMENT ON COLUMN m_pay_items.company_id IS '会社ID';
COMMENT ON COLUMN m_pay_items.code IS '項目コード';
COMMENT ON COLUMN m_pay_items.name IS '項目名';
COMMENT ON COLUMN m_pay_items.type IS '項目種別';
COMMENT ON COLUMN m_pay_items.taxable IS '課税対象フラグ';
COMMENT ON COLUMN m_pay_items.sort_order IS '表示順';

COMMENT ON COLUMN m_pay_items.created_at IS '作成日時';
COMMENT ON COLUMN m_pay_items.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN m_pay_items.updated_at IS '更新日時';
COMMENT ON COLUMN m_pay_items.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN m_pay_items.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN m_pay_items.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_m_pay_items_company_code
  ON m_pay_items(company_id, code)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_m_pay_items
BEFORE UPDATE ON m_pay_items
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE t_payslips (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  payroll_period_id uuid NOT NULL REFERENCES t_payroll_periods(id),
  employee_id uuid NOT NULL REFERENCES m_employees(id),

  status text NOT NULL DEFAULT 'draft', -- draft|confirmed|paid
  gross numeric(12,2) NOT NULL DEFAULT 0,
  total_deduction numeric(12,2) NOT NULL DEFAULT 0,
  net numeric(12,2) NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_payslips IS '給与明細（ヘッダ）';
COMMENT ON COLUMN t_payslips.company_id IS '会社ID';
COMMENT ON COLUMN t_payslips.payroll_period_id IS '給与期間ID';
COMMENT ON COLUMN t_payslips.employee_id IS '従業員ID';
COMMENT ON COLUMN t_payslips.status IS '状態（draft|confirmed|paid）';
COMMENT ON COLUMN t_payslips.gross IS '総支給';
COMMENT ON COLUMN t_payslips.total_deduction IS '控除合計';
COMMENT ON COLUMN t_payslips.net IS '差引支給額';

COMMENT ON COLUMN t_payslips.created_at IS '作成日時';
COMMENT ON COLUMN t_payslips.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_payslips.updated_at IS '更新日時';
COMMENT ON COLUMN t_payslips.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_payslips.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_payslips.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_t_payslips_company_period_employee
  ON t_payslips(company_id, payroll_period_id, employee_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_payslips
BEFORE UPDATE ON t_payslips
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE r_payslip_lines (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  payslip_id uuid NOT NULL REFERENCES t_payslips(id),
  pay_item_id uuid NOT NULL REFERENCES m_pay_items(id),

  quantity numeric(12,4) NOT NULL DEFAULT 0,
  unit_amount numeric(12,2) NOT NULL DEFAULT 0,
  amount numeric(12,2) NOT NULL DEFAULT 0,
  note text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE r_payslip_lines IS '給与明細（行）';
COMMENT ON COLUMN r_payslip_lines.company_id IS '会社ID';
COMMENT ON COLUMN r_payslip_lines.payslip_id IS '給与明細ID';
COMMENT ON COLUMN r_payslip_lines.pay_item_id IS '給与項目ID';
COMMENT ON COLUMN r_payslip_lines.quantity IS '数量';
COMMENT ON COLUMN r_payslip_lines.unit_amount IS '単価';
COMMENT ON COLUMN r_payslip_lines.amount IS '金額';
COMMENT ON COLUMN r_payslip_lines.note IS '備考';

COMMENT ON COLUMN r_payslip_lines.created_at IS '作成日時';
COMMENT ON COLUMN r_payslip_lines.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN r_payslip_lines.updated_at IS '更新日時';
COMMENT ON COLUMN r_payslip_lines.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN r_payslip_lines.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN r_payslip_lines.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_r_payslip_lines_company_payslip
  ON r_payslip_lines(company_id, payslip_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_r_payslip_lines
BEFORE UPDATE ON r_payslip_lines
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE t_payments (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  payroll_period_id uuid NOT NULL REFERENCES t_payroll_periods(id),
  pay_date date NOT NULL,
  method text NOT NULL, -- bank_transfer|cash
  status text NOT NULL DEFAULT 'scheduled', -- scheduled|processing|done|failed

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_payments IS '支払バッチ（ヘッダ）';
COMMENT ON COLUMN t_payments.company_id IS '会社ID';
COMMENT ON COLUMN t_payments.payroll_period_id IS '給与期間ID';
COMMENT ON COLUMN t_payments.pay_date IS '支払日';
COMMENT ON COLUMN t_payments.method IS '支払方法';
COMMENT ON COLUMN t_payments.status IS '状態（scheduled|processing|done|failed）';

COMMENT ON COLUMN t_payments.created_at IS '作成日時';
COMMENT ON COLUMN t_payments.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_payments.updated_at IS '更新日時';
COMMENT ON COLUMN t_payments.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_payments.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_payments.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_t_payments_company_period
  ON t_payments(company_id, payroll_period_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_payments
BEFORE UPDATE ON t_payments
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE r_payment_lines (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  payment_id uuid NOT NULL REFERENCES t_payments(id),
  payslip_id uuid NOT NULL REFERENCES t_payslips(id),
  amount numeric(12,2) NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE r_payment_lines IS '支払バッチ（行）';
COMMENT ON COLUMN r_payment_lines.company_id IS '会社ID';
COMMENT ON COLUMN r_payment_lines.payment_id IS '支払バッチID';
COMMENT ON COLUMN r_payment_lines.payslip_id IS '給与明細ID';
COMMENT ON COLUMN r_payment_lines.amount IS '支払金額';

COMMENT ON COLUMN r_payment_lines.created_at IS '作成日時';
COMMENT ON COLUMN r_payment_lines.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN r_payment_lines.updated_at IS '更新日時';
COMMENT ON COLUMN r_payment_lines.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN r_payment_lines.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN r_payment_lines.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_r_payment_lines_company_payment
  ON r_payment_lines(company_id, payment_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_r_payment_lines
BEFORE UPDATE ON r_payment_lines
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 7) 監査ログ / ジョブログ（RDSに保持）
-- =========================================================

CREATE TABLE h_audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  actor_user_id uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  action text NOT NULL,
  target_type text NOT NULL,
  target_id uuid NOT NULL,

  before_data jsonb,
  after_data jsonb,
  request_id text,
  happened_at timestamptz NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE h_audit_logs IS '監査ログ（業務データ変更の追跡）';
COMMENT ON COLUMN h_audit_logs.company_id IS '会社ID';
COMMENT ON COLUMN h_audit_logs.actor_user_id IS '実行者ユーザーID';
COMMENT ON COLUMN h_audit_logs.action IS '操作';
COMMENT ON COLUMN h_audit_logs.target_type IS '対象テーブル名（m_users|m_employees等）';
COMMENT ON COLUMN h_audit_logs.target_id IS '対象ID';
COMMENT ON COLUMN h_audit_logs.before_data IS '変更前データ';
COMMENT ON COLUMN h_audit_logs.after_data IS '変更後データ';
COMMENT ON COLUMN h_audit_logs.request_id IS 'リクエストID';
COMMENT ON COLUMN h_audit_logs.happened_at IS '発生日時';

COMMENT ON COLUMN h_audit_logs.created_at IS '作成日時';
COMMENT ON COLUMN h_audit_logs.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN h_audit_logs.updated_at IS '更新日時';
COMMENT ON COLUMN h_audit_logs.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN h_audit_logs.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN h_audit_logs.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_h_audit_logs_company_time
  ON h_audit_logs(company_id, happened_at);

CREATE TRIGGER set_updated_at_h_audit_logs
BEFORE UPDATE ON h_audit_logs
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE t_job_runs (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  job_type text NOT NULL, -- payroll_calculation|payroll_finalize|export_csv|import_csv|etc
  status text NOT NULL DEFAULT 'running', -- running|success|failed|canceled
  request_id text,

  started_at timestamptz NOT NULL,
  finished_at timestamptz,

  params jsonb,
  result jsonb,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_job_runs IS 'ジョブ実行（業務バッチのトランザクション）';
COMMENT ON COLUMN t_job_runs.company_id IS '会社ID';
COMMENT ON COLUMN t_job_runs.job_type IS 'ジョブ種別';
COMMENT ON COLUMN t_job_runs.status IS '状態（running|success|failed|canceled）';
COMMENT ON COLUMN t_job_runs.request_id IS 'リクエストID';
COMMENT ON COLUMN t_job_runs.started_at IS '開始日時';
COMMENT ON COLUMN t_job_runs.finished_at IS '終了日時';
COMMENT ON COLUMN t_job_runs.params IS 'パラメータ';
COMMENT ON COLUMN t_job_runs.result IS '結果';

COMMENT ON COLUMN t_job_runs.created_at IS '作成日時';
COMMENT ON COLUMN t_job_runs.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN t_job_runs.updated_at IS '更新日時';
COMMENT ON COLUMN t_job_runs.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN t_job_runs.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN t_job_runs.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_t_job_runs_company_time
  ON t_job_runs(company_id, started_at);

CREATE TRIGGER set_updated_at_t_job_runs
BEFORE UPDATE ON t_job_runs
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE h_job_run_events (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  job_run_id uuid NOT NULL REFERENCES t_job_runs(id),
  level text NOT NULL, -- info|warn|error
  message text NOT NULL,
  metadata jsonb,
  happened_at timestamptz NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE h_job_run_events IS 'ジョブ実行ログ（業務バッチの詳細イベント）';
COMMENT ON COLUMN h_job_run_events.company_id IS '会社ID';
COMMENT ON COLUMN h_job_run_events.job_run_id IS 'ジョブ実行ID';
COMMENT ON COLUMN h_job_run_events.level IS 'レベル';
COMMENT ON COLUMN h_job_run_events.message IS 'メッセージ';
COMMENT ON COLUMN h_job_run_events.metadata IS 'メタデータ';
COMMENT ON COLUMN h_job_run_events.happened_at IS '発生日時';

COMMENT ON COLUMN h_job_run_events.created_at IS '作成日時';
COMMENT ON COLUMN h_job_run_events.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN h_job_run_events.updated_at IS '更新日時';
COMMENT ON COLUMN h_job_run_events.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN h_job_run_events.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN h_job_run_events.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_h_job_run_events_company_job
  ON h_job_run_events(company_id, job_run_id, happened_at);

CREATE TRIGGER set_updated_at_h_job_run_events
BEFORE UPDATE ON h_job_run_events
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- =========================================================
-- 補足（重要）:
-- created_by/updated_by を NOT NULL + FK にしているため、
-- 初期データ投入では "systemユーザー" を最初に作る運用が必要です。
-- DEFERRABLE INITIALLY DEFERRED を付けてあるので、同一トランザクションで
-- 自己参照（created_by = 自分）も成立させやすくしています。
-- =========================================================

-- =========================================================
-- 初期データ投入
-- =========================================================
-- 注意: 初期データ投入は別ファイル（20261229_init_data.sql）で実行してください
-- 実行方法:
--   $env:OutputEncoding = [System.Text.Encoding]::UTF8; Get-Content database/migrations/20261229_init_data.sql -Encoding UTF8 | docker exec -i auratime-postgres psql -U postgres -d auratime
-- =========================================================
