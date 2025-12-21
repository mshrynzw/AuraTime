-- =========================================================
-- 000_init.sql (PostgreSQL / RDS)
-- Multi-tenant: 会社=テナント
-- GROUPSに部署/勤務地/コストセンター等を統合
-- SYSTEM_LOGSは外部（CloudWatch等）想定のためDBには作らない
-- =========================================================

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
  unix_ts_ms := EXTRACT(EPOCH FROM now()) * 1000;
  
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
                0, (unix_ts_ms >> 40)::int & 255
              ),
              1, (unix_ts_ms >> 32)::int & 255
            ),
            2, (unix_ts_ms >> 24)::int & 255
          ),
          3, (unix_ts_ms >> 16)::int & 255
        ),
        4, (unix_ts_ms >> 8)::int & 255
      ),
      5, unix_ts_ms::int & 255
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

-- =========================================================
-- 1) マスタ系
-- =========================================================

-- companies（会社=テナント）
CREATE TABLE companies (
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

COMMENT ON TABLE companies IS '会社（テナント）';

COMMENT ON COLUMN companies.id IS '会社ID';
COMMENT ON COLUMN companies.name IS '会社名';
COMMENT ON COLUMN companies.code IS '会社コード';
COMMENT ON COLUMN companies.timezone IS 'タイムゾーン';
COMMENT ON COLUMN companies.currency IS '通貨';

COMMENT ON COLUMN companies.created_at IS '作成日時';
COMMENT ON COLUMN companies.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN companies.updated_at IS '更新日時';
COMMENT ON COLUMN companies.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN companies.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN companies.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_companies_code
  ON companies(code)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_companies
BEFORE UPDATE ON companies
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- users（ユーザー）
CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  email text NOT NULL,
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

COMMENT ON TABLE users IS 'ユーザー';

COMMENT ON COLUMN users.id IS 'ユーザーID';
COMMENT ON COLUMN users.email IS 'メールアドレス';
COMMENT ON COLUMN users.family_name IS '姓';
COMMENT ON COLUMN users.first_name IS '名';
COMMENT ON COLUMN users.family_name_kana IS '姓（カナ）';
COMMENT ON COLUMN users.first_name_kana IS '名（カナ）';
COMMENT ON COLUMN users.status IS '状態（active|inactive|locked）';

COMMENT ON COLUMN users.created_at IS '作成日時';
COMMENT ON COLUMN users.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN users.updated_at IS '更新日時';
COMMENT ON COLUMN users.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN users.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN users.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_users_email
  ON users(email)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_users
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- FK（循環を避けつつ NOT NULL を維持するため DEFERRABLE）
ALTER TABLE companies
  ADD CONSTRAINT fk_companies_created_by
  FOREIGN KEY (created_by) REFERENCES users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE companies
  ADD CONSTRAINT fk_companies_updated_by
  FOREIGN KEY (updated_by) REFERENCES users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE companies
  ADD CONSTRAINT fk_companies_deleted_by
  FOREIGN KEY (deleted_by) REFERENCES users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE users
  ADD CONSTRAINT fk_users_created_by
  FOREIGN KEY (created_by) REFERENCES users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE users
  ADD CONSTRAINT fk_users_updated_by
  FOREIGN KEY (updated_by) REFERENCES users(id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE users
  ADD CONSTRAINT fk_users_deleted_by
  FOREIGN KEY (deleted_by) REFERENCES users(id)
  DEFERRABLE INITIALLY DEFERRED;


-- company_memberships（会社所属/権限）
CREATE TABLE company_memberships (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES companies(id),
  user_id uuid NOT NULL REFERENCES users(id),
  role text NOT NULL, -- system_admin|admin|manager|employee
  joined_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE company_memberships IS '会社所属（権限）';
COMMENT ON COLUMN company_memberships.company_id IS '会社ID';
COMMENT ON COLUMN company_memberships.user_id IS 'ユーザーID';
COMMENT ON COLUMN company_memberships.role IS '権限ロール（system_admin|admin|manager|employee）';
COMMENT ON COLUMN company_memberships.joined_at IS '参加日時';

COMMENT ON COLUMN company_memberships.created_at IS '作成日時';
COMMENT ON COLUMN company_memberships.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN company_memberships.updated_at IS '更新日時';
COMMENT ON COLUMN company_memberships.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN company_memberships.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN company_memberships.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_company_memberships_company_user
  ON company_memberships(company_id, user_id)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_company_memberships_company
  ON company_memberships(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_company_memberships
BEFORE UPDATE ON company_memberships
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- groups（部署/勤務地/チーム/コストセンター等の統合）
CREATE TABLE groups (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES companies(id),
  type text NOT NULL, -- department|work_location|team|cost_center|custom
  name text NOT NULL,
  code text,
  parent_group_id uuid REFERENCES groups(id),
  sort_order int NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE groups IS 'グループ（部署/勤務地/チーム/コストセンター等）';
COMMENT ON COLUMN groups.company_id IS '会社ID';
COMMENT ON COLUMN groups.type IS 'グループ種別';
COMMENT ON COLUMN groups.name IS 'グループ名';
COMMENT ON COLUMN groups.code IS 'グループコード';
COMMENT ON COLUMN groups.parent_group_id IS '親グループID';
COMMENT ON COLUMN groups.sort_order IS '表示順';

COMMENT ON COLUMN groups.created_at IS '作成日時';
COMMENT ON COLUMN groups.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN groups.updated_at IS '更新日時';
COMMENT ON COLUMN groups.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN groups.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN groups.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_groups_company_type
  ON groups(company_id, type)
  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_groups_company_type_code
  ON groups(company_id, type, code)
  WHERE deleted_at IS NULL AND code IS NOT NULL;

CREATE TRIGGER set_updated_at_groups
BEFORE UPDATE ON groups
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- employees（従業員）
CREATE TABLE employees (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES companies(id),
  user_id uuid REFERENCES users(id),
  employee_no text NOT NULL,
  employment_type text NOT NULL, -- fulltime|parttime|contract
  hire_date date NOT NULL,
  termination_date date,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE employees IS '従業員';
COMMENT ON COLUMN employees.company_id IS '会社ID';
COMMENT ON COLUMN employees.user_id IS 'ユーザーID（ログイン紐付け）';
COMMENT ON COLUMN employees.employee_no IS '社員番号';
COMMENT ON COLUMN employees.employment_type IS '雇用区分';
COMMENT ON COLUMN employees.hire_date IS '入社日';
COMMENT ON COLUMN employees.termination_date IS '退職日';

COMMENT ON COLUMN employees.created_at IS '作成日時';
COMMENT ON COLUMN employees.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN employees.updated_at IS '更新日時';
COMMENT ON COLUMN employees.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN employees.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN employees.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_employees_company_employee_no
  ON employees(company_id, employee_no)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_employees_company
  ON employees(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_employees
BEFORE UPDATE ON employees
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- employee_groups（所属：履歴/兼務/役割）
CREATE TABLE employee_groups (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),

  company_id uuid NOT NULL REFERENCES companies(id),
  employee_id uuid NOT NULL REFERENCES employees(id),
  group_id uuid NOT NULL REFERENCES groups(id),

  role_in_group text NOT NULL DEFAULT 'member', -- member|leader
  valid_from date NOT NULL,
  valid_to date,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE employee_groups IS '従業員のグループ所属（履歴・兼務対応）';
COMMENT ON COLUMN employee_groups.company_id IS '会社ID';
COMMENT ON COLUMN employee_groups.employee_id IS '従業員ID';
COMMENT ON COLUMN employee_groups.group_id IS 'グループID';
COMMENT ON COLUMN employee_groups.role_in_group IS 'グループ内役割（leader|member）';
COMMENT ON COLUMN employee_groups.valid_from IS '有効開始日';
COMMENT ON COLUMN employee_groups.valid_to IS '有効終了日';

COMMENT ON COLUMN employee_groups.created_at IS '作成日時';
COMMENT ON COLUMN employee_groups.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN employee_groups.updated_at IS '更新日時';
COMMENT ON COLUMN employee_groups.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN employee_groups.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN employee_groups.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_employee_groups_company_employee
  ON employee_groups(company_id, employee_id)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_employee_groups_company_group
  ON employee_groups(company_id, group_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_employee_groups
BEFORE UPDATE ON employee_groups
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 2) 勤怠
-- =========================================================

-- shift_templates（シフトテンプレ）
CREATE TABLE shift_templates (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  name text NOT NULL,
  default_start time,
  default_end time,
  break_minutes int NOT NULL DEFAULT 0,
  is_night_shift boolean NOT NULL DEFAULT false,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE shift_templates IS 'シフトテンプレート';
COMMENT ON COLUMN shift_templates.company_id IS '会社ID';
COMMENT ON COLUMN shift_templates.name IS '名称';
COMMENT ON COLUMN shift_templates.default_start IS '開始時刻（規定）';
COMMENT ON COLUMN shift_templates.default_end IS '終了時刻（規定）';
COMMENT ON COLUMN shift_templates.break_minutes IS '休憩分（規定）';
COMMENT ON COLUMN shift_templates.is_night_shift IS '夜勤フラグ';

COMMENT ON COLUMN shift_templates.created_at IS '作成日時';
COMMENT ON COLUMN shift_templates.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN shift_templates.updated_at IS '更新日時';
COMMENT ON COLUMN shift_templates.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN shift_templates.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN shift_templates.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_shift_templates_company
  ON shift_templates(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_shift_templates
BEFORE UPDATE ON shift_templates
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- shift_break_templates（シフト休憩テンプレ）
CREATE TABLE shift_break_templates (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),
  shift_template_id uuid NOT NULL REFERENCES shift_templates(id),

  break_start time NOT NULL,
  break_end time NOT NULL,
  break_name text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,

  CONSTRAINT ck_break_time_range CHECK (break_start < break_end)
);

COMMENT ON TABLE shift_break_templates IS 'シフト休憩テンプレート';
COMMENT ON COLUMN shift_break_templates.company_id IS '会社ID';
COMMENT ON COLUMN shift_break_templates.shift_template_id IS 'シフトテンプレートID';
COMMENT ON COLUMN shift_break_templates.break_start IS '休憩開始時刻';
COMMENT ON COLUMN shift_break_templates.break_end IS '休憩終了時刻';
COMMENT ON COLUMN shift_break_templates.break_name IS '休憩名';

CREATE INDEX ix_shift_break_templates_shift
  ON shift_break_templates(shift_template_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_shift_break_templates
BEFORE UPDATE ON shift_break_templates
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- shift_assignments（シフト割当）
CREATE TABLE shift_assignments (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  employee_id uuid NOT NULL REFERENCES employees(id),
  work_date date NOT NULL,
  shift_template_id uuid REFERENCES shift_templates(id),
  work_group_id uuid REFERENCES groups(id), -- GROUPS(type=work_location等)

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE shift_assignments IS 'シフト割当';
COMMENT ON COLUMN shift_assignments.company_id IS '会社ID';
COMMENT ON COLUMN shift_assignments.employee_id IS '従業員ID';
COMMENT ON COLUMN shift_assignments.work_date IS '勤務日';
COMMENT ON COLUMN shift_assignments.shift_template_id IS 'シフトテンプレートID';
COMMENT ON COLUMN shift_assignments.work_group_id IS '勤務グループID（勤務地など）';

COMMENT ON COLUMN shift_assignments.created_at IS '作成日時';
COMMENT ON COLUMN shift_assignments.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN shift_assignments.updated_at IS '更新日時';
COMMENT ON COLUMN shift_assignments.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN shift_assignments.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN shift_assignments.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_shift_assignments_company_employee_date
  ON shift_assignments(company_id, employee_id, work_date)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_shift_assignments
BEFORE UPDATE ON shift_assignments
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- time_clock_events（打刻：生ログ）
CREATE TABLE time_clock_events (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  employee_id uuid NOT NULL REFERENCES employees(id),
  happened_at timestamptz NOT NULL,
  type text NOT NULL,   -- in|out|break_in|break_out
  source text NOT NULL, -- web|mobile|ic_card|admin (ic_cardは将来対応)
  note text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE time_clock_events IS '打刻イベント（生ログ）';
COMMENT ON COLUMN time_clock_events.company_id IS '会社ID';
COMMENT ON COLUMN time_clock_events.employee_id IS '従業員ID';
COMMENT ON COLUMN time_clock_events.happened_at IS '発生日時';
COMMENT ON COLUMN time_clock_events.type IS '打刻種別（in|out|break_in|break_out）';
COMMENT ON COLUMN time_clock_events.source IS '入力元（web|mobile|ic_card|admin）。ic_cardは将来対応。';
COMMENT ON COLUMN time_clock_events.note IS '備考（遅延理由、位置情報等）';

COMMENT ON COLUMN time_clock_events.created_at IS '作成日時';
COMMENT ON COLUMN time_clock_events.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN time_clock_events.updated_at IS '更新日時';
COMMENT ON COLUMN time_clock_events.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN time_clock_events.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN time_clock_events.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_time_clock_events_company_employee_time
  ON time_clock_events(company_id, employee_id, happened_at)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_time_clock_events
BEFORE UPDATE ON time_clock_events
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- time_records（勤怠：日次集計/申請承認）
CREATE TABLE time_records (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  employee_id uuid NOT NULL REFERENCES employees(id),
  work_date date NOT NULL,

  start_at timestamptz,
  end_at timestamptz,
  break_minutes int NOT NULL DEFAULT 0,
  work_minutes int NOT NULL DEFAULT 0,
  overtime_minutes int NOT NULL DEFAULT 0,
  night_minutes int NOT NULL DEFAULT 0,

  status text NOT NULL DEFAULT 'draft', -- draft|submitted|approved|rejected|locked
  submitted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  submitted_at timestamptz,
  approved_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  approved_at timestamptz,

  cost_group_id uuid REFERENCES groups(id), -- GROUPS(type=cost_center等)

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE time_records IS '勤怠レコード（集計・申請承認）';
COMMENT ON COLUMN time_records.company_id IS '会社ID';
COMMENT ON COLUMN time_records.employee_id IS '従業員ID';
COMMENT ON COLUMN time_records.work_date IS '勤務日';
COMMENT ON COLUMN time_records.start_at IS '開始日時';
COMMENT ON COLUMN time_records.end_at IS '終了日時';
COMMENT ON COLUMN time_records.break_minutes IS '休憩分';
COMMENT ON COLUMN time_records.work_minutes IS '労働分';
COMMENT ON COLUMN time_records.overtime_minutes IS '残業分';
COMMENT ON COLUMN time_records.night_minutes IS '深夜分';
COMMENT ON COLUMN time_records.status IS '状態（draft|submitted|approved|rejected|locked）';
COMMENT ON COLUMN time_records.submitted_by IS '申請者ユーザーID';
COMMENT ON COLUMN time_records.submitted_at IS '申請日時';
COMMENT ON COLUMN time_records.approved_by IS '承認者ユーザーID';
COMMENT ON COLUMN time_records.approved_at IS '承認日時';
COMMENT ON COLUMN time_records.cost_group_id IS 'コストセンターID（配賦先）';

COMMENT ON COLUMN time_records.created_at IS '作成日時';
COMMENT ON COLUMN time_records.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN time_records.updated_at IS '更新日時';
COMMENT ON COLUMN time_records.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN time_records.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN time_records.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_time_records_company_employee_date
  ON time_records(company_id, employee_id, work_date)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_time_records_company_date
  ON time_records(company_id, work_date)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_time_records
BEFORE UPDATE ON time_records
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 3) 承認（汎用）
-- =========================================================

CREATE TABLE approval_requests (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  target_type text NOT NULL, -- time_record|leave_request|other
  target_id uuid NOT NULL,

  requester_user_id uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  current_approver_user_id uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  status text NOT NULL DEFAULT 'pending', -- pending|approved|rejected|canceled
  requested_at timestamptz NOT NULL,
  resolved_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE approval_requests IS '承認リクエスト';
COMMENT ON COLUMN approval_requests.company_id IS '会社ID';
COMMENT ON COLUMN approval_requests.target_type IS '対象種別（time_record|leave_request等）';
COMMENT ON COLUMN approval_requests.target_id IS '対象ID';
COMMENT ON COLUMN approval_requests.requester_user_id IS '申請者ユーザーID';
COMMENT ON COLUMN approval_requests.current_approver_user_id IS '現在承認者ユーザーID';
COMMENT ON COLUMN approval_requests.status IS '状態（pending|approved|rejected|canceled）';
COMMENT ON COLUMN approval_requests.requested_at IS '申請日時';
COMMENT ON COLUMN approval_requests.resolved_at IS '確定日時';

COMMENT ON COLUMN approval_requests.created_at IS '作成日時';
COMMENT ON COLUMN approval_requests.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN approval_requests.updated_at IS '更新日時';
COMMENT ON COLUMN approval_requests.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN approval_requests.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN approval_requests.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_approval_requests_company_status
  ON approval_requests(company_id, status)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_approval_requests
BEFORE UPDATE ON approval_requests
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE approval_steps (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  approval_request_id uuid NOT NULL REFERENCES approval_requests(id),
  step_no int NOT NULL,
  approver_user_id uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,

  status text NOT NULL DEFAULT 'pending', -- pending|approved|rejected|skipped
  acted_at timestamptz,
  comment text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE approval_steps IS '承認ステップ';
COMMENT ON COLUMN approval_steps.company_id IS '会社ID';
COMMENT ON COLUMN approval_steps.approval_request_id IS '承認リクエストID';
COMMENT ON COLUMN approval_steps.step_no IS 'ステップ番号';
COMMENT ON COLUMN approval_steps.approver_user_id IS '承認者ユーザーID';
COMMENT ON COLUMN approval_steps.status IS '状態（pending|approved|rejected|skipped）';
COMMENT ON COLUMN approval_steps.acted_at IS '操作日時';
COMMENT ON COLUMN approval_steps.comment IS 'コメント';

COMMENT ON COLUMN approval_steps.created_at IS '作成日時';
COMMENT ON COLUMN approval_steps.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN approval_steps.updated_at IS '更新日時';
COMMENT ON COLUMN approval_steps.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN approval_steps.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN approval_steps.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_approval_steps_request_step
  ON approval_steps(approval_request_id, step_no)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_approval_steps
BEFORE UPDATE ON approval_steps
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 4) 休暇
-- =========================================================

CREATE TABLE leave_types (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  name text NOT NULL,
  code text NOT NULL,
  is_paid boolean NOT NULL DEFAULT false,
  requires_approval boolean NOT NULL DEFAULT true,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE leave_types IS '休暇種別';
COMMENT ON COLUMN leave_types.company_id IS '会社ID';
COMMENT ON COLUMN leave_types.name IS '休暇名';
COMMENT ON COLUMN leave_types.code IS '休暇コード';
COMMENT ON COLUMN leave_types.is_paid IS '有給フラグ';
COMMENT ON COLUMN leave_types.requires_approval IS '承認要否';

COMMENT ON COLUMN leave_types.created_at IS '作成日時';
COMMENT ON COLUMN leave_types.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN leave_types.updated_at IS '更新日時';
COMMENT ON COLUMN leave_types.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN leave_types.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN leave_types.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_leave_types_company_code
  ON leave_types(company_id, code)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_leave_types
BEFORE UPDATE ON leave_types
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE leave_requests (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  employee_id uuid NOT NULL REFERENCES employees(id),
  leave_type_id uuid NOT NULL REFERENCES leave_types(id),

  start_date date NOT NULL,
  end_date date NOT NULL,
  days numeric(6,2) NOT NULL,
  status text NOT NULL DEFAULT 'draft', -- draft|submitted|approved|rejected|canceled
  reason text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE leave_requests IS '休暇申請';
COMMENT ON COLUMN leave_requests.company_id IS '会社ID';
COMMENT ON COLUMN leave_requests.employee_id IS '従業員ID';
COMMENT ON COLUMN leave_requests.leave_type_id IS '休暇種別ID';
COMMENT ON COLUMN leave_requests.start_date IS '開始日';
COMMENT ON COLUMN leave_requests.end_date IS '終了日';
COMMENT ON COLUMN leave_requests.days IS '日数';
COMMENT ON COLUMN leave_requests.status IS '状態（draft|submitted|approved|rejected|canceled）';
COMMENT ON COLUMN leave_requests.reason IS '理由';

COMMENT ON COLUMN leave_requests.created_at IS '作成日時';
COMMENT ON COLUMN leave_requests.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN leave_requests.updated_at IS '更新日時';
COMMENT ON COLUMN leave_requests.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN leave_requests.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN leave_requests.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_leave_requests_company_employee_date
  ON leave_requests(company_id, employee_id, start_date, end_date)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_leave_requests
BEFORE UPDATE ON leave_requests
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 5) 給与
-- =========================================================

CREATE TABLE payroll_calendars (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  name text NOT NULL,
  closing_day int NOT NULL, -- 1-31 or 0=EOM
  payday_day int NOT NULL,  -- 1-31 or 0=EOM

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE payroll_calendars IS '給与カレンダー';
COMMENT ON COLUMN payroll_calendars.company_id IS '会社ID';
COMMENT ON COLUMN payroll_calendars.name IS '名称';
COMMENT ON COLUMN payroll_calendars.closing_day IS '締日';
COMMENT ON COLUMN payroll_calendars.payday_day IS '支払日';

COMMENT ON COLUMN payroll_calendars.created_at IS '作成日時';
COMMENT ON COLUMN payroll_calendars.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN payroll_calendars.updated_at IS '更新日時';
COMMENT ON COLUMN payroll_calendars.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN payroll_calendars.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN payroll_calendars.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_payroll_calendars_company
  ON payroll_calendars(company_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_payroll_calendars
BEFORE UPDATE ON payroll_calendars
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE payroll_periods (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  payroll_calendar_id uuid NOT NULL REFERENCES payroll_calendars(id),
  period_start date NOT NULL,
  period_end date NOT NULL,
  closing_date date NOT NULL,
  pay_date date NOT NULL,
  status text NOT NULL DEFAULT 'open', -- open|closed|calculated|finalized

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE payroll_periods IS '給与期間（締め単位）';
COMMENT ON COLUMN payroll_periods.company_id IS '会社ID';
COMMENT ON COLUMN payroll_periods.payroll_calendar_id IS '給与カレンダーID';
COMMENT ON COLUMN payroll_periods.period_start IS '期間開始日';
COMMENT ON COLUMN payroll_periods.period_end IS '期間終了日';
COMMENT ON COLUMN payroll_periods.closing_date IS '締日（実日）';
COMMENT ON COLUMN payroll_periods.pay_date IS '支払日（実日）';
COMMENT ON COLUMN payroll_periods.status IS '状態（open|closed|calculated|finalized）';

COMMENT ON COLUMN payroll_periods.created_at IS '作成日時';
COMMENT ON COLUMN payroll_periods.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN payroll_periods.updated_at IS '更新日時';
COMMENT ON COLUMN payroll_periods.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN payroll_periods.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN payroll_periods.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_payroll_periods_company_calendar_range
  ON payroll_periods(company_id, payroll_calendar_id, period_start, period_end)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_payroll_periods
BEFORE UPDATE ON payroll_periods
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE compensation_plans (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  employee_id uuid NOT NULL REFERENCES employees(id),
  pay_type text NOT NULL, -- monthly|hourly
  base_salary numeric(12,2),
  hourly_rate numeric(12,2),
  overtime_rate numeric(12,2),
  night_rate numeric(12,2),
  effective_from date NOT NULL,
  effective_to date,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE compensation_plans IS '報酬条件（履歴）';
COMMENT ON COLUMN compensation_plans.company_id IS '会社ID';
COMMENT ON COLUMN compensation_plans.employee_id IS '従業員ID';
COMMENT ON COLUMN compensation_plans.pay_type IS '給与形態';
COMMENT ON COLUMN compensation_plans.base_salary IS '基本給（月給）';
COMMENT ON COLUMN compensation_plans.hourly_rate IS '時給';
COMMENT ON COLUMN compensation_plans.overtime_rate IS '残業単価（係数/金額）';
COMMENT ON COLUMN compensation_plans.night_rate IS '深夜単価（係数/金額）';
COMMENT ON COLUMN compensation_plans.effective_from IS '適用開始日';
COMMENT ON COLUMN compensation_plans.effective_to IS '適用終了日';

COMMENT ON COLUMN compensation_plans.created_at IS '作成日時';
COMMENT ON COLUMN compensation_plans.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN compensation_plans.updated_at IS '更新日時';
COMMENT ON COLUMN compensation_plans.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN compensation_plans.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN compensation_plans.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_compensation_plans_company_employee_effective
  ON compensation_plans(company_id, employee_id, effective_from)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_compensation_plans
BEFORE UPDATE ON compensation_plans
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE pay_items (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  code text NOT NULL,
  name text NOT NULL,
  type text NOT NULL, -- earning|deduction|employer_cost
  taxable boolean NOT NULL DEFAULT true,
  sort_order int NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE pay_items IS '給与項目（支給/控除など）';
COMMENT ON COLUMN pay_items.company_id IS '会社ID';
COMMENT ON COLUMN pay_items.code IS '項目コード';
COMMENT ON COLUMN pay_items.name IS '項目名';
COMMENT ON COLUMN pay_items.type IS '項目種別';
COMMENT ON COLUMN pay_items.taxable IS '課税対象フラグ';
COMMENT ON COLUMN pay_items.sort_order IS '表示順';

COMMENT ON COLUMN pay_items.created_at IS '作成日時';
COMMENT ON COLUMN pay_items.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN pay_items.updated_at IS '更新日時';
COMMENT ON COLUMN pay_items.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN pay_items.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN pay_items.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_pay_items_company_code
  ON pay_items(company_id, code)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_pay_items
BEFORE UPDATE ON pay_items
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE payslips (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  payroll_period_id uuid NOT NULL REFERENCES payroll_periods(id),
  employee_id uuid NOT NULL REFERENCES employees(id),

  status text NOT NULL DEFAULT 'draft', -- draft|confirmed|paid
  gross numeric(12,2) NOT NULL DEFAULT 0,
  total_deduction numeric(12,2) NOT NULL DEFAULT 0,
  net numeric(12,2) NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE payslips IS '給与明細（ヘッダ）';
COMMENT ON COLUMN payslips.company_id IS '会社ID';
COMMENT ON COLUMN payslips.payroll_period_id IS '給与期間ID';
COMMENT ON COLUMN payslips.employee_id IS '従業員ID';
COMMENT ON COLUMN payslips.status IS '状態（draft|confirmed|paid）';
COMMENT ON COLUMN payslips.gross IS '総支給';
COMMENT ON COLUMN payslips.total_deduction IS '控除合計';
COMMENT ON COLUMN payslips.net IS '差引支給額';

COMMENT ON COLUMN payslips.created_at IS '作成日時';
COMMENT ON COLUMN payslips.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN payslips.updated_at IS '更新日時';
COMMENT ON COLUMN payslips.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN payslips.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN payslips.deleted_by IS '削除者ユーザーID';

CREATE UNIQUE INDEX uq_payslips_company_period_employee
  ON payslips(company_id, payroll_period_id, employee_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_payslips
BEFORE UPDATE ON payslips
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE payslip_lines (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  payslip_id uuid NOT NULL REFERENCES payslips(id),
  pay_item_id uuid NOT NULL REFERENCES pay_items(id),

  quantity numeric(12,4) NOT NULL DEFAULT 0,
  unit_amount numeric(12,2) NOT NULL DEFAULT 0,
  amount numeric(12,2) NOT NULL DEFAULT 0,
  note text,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE payslip_lines IS '給与明細（行）';
COMMENT ON COLUMN payslip_lines.company_id IS '会社ID';
COMMENT ON COLUMN payslip_lines.payslip_id IS '給与明細ID';
COMMENT ON COLUMN payslip_lines.pay_item_id IS '給与項目ID';
COMMENT ON COLUMN payslip_lines.quantity IS '数量';
COMMENT ON COLUMN payslip_lines.unit_amount IS '単価';
COMMENT ON COLUMN payslip_lines.amount IS '金額';
COMMENT ON COLUMN payslip_lines.note IS '備考';

COMMENT ON COLUMN payslip_lines.created_at IS '作成日時';
COMMENT ON COLUMN payslip_lines.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN payslip_lines.updated_at IS '更新日時';
COMMENT ON COLUMN payslip_lines.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN payslip_lines.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN payslip_lines.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_payslip_lines_company_payslip
  ON payslip_lines(company_id, payslip_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_payslip_lines
BEFORE UPDATE ON payslip_lines
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE payments (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  payroll_period_id uuid NOT NULL REFERENCES payroll_periods(id),
  pay_date date NOT NULL,
  method text NOT NULL, -- bank_transfer|cash
  status text NOT NULL DEFAULT 'scheduled', -- scheduled|processing|done|failed

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE payments IS '支払バッチ（ヘッダ）';
COMMENT ON COLUMN payments.company_id IS '会社ID';
COMMENT ON COLUMN payments.payroll_period_id IS '給与期間ID';
COMMENT ON COLUMN payments.pay_date IS '支払日';
COMMENT ON COLUMN payments.method IS '支払方法';
COMMENT ON COLUMN payments.status IS '状態（scheduled|processing|done|failed）';

COMMENT ON COLUMN payments.created_at IS '作成日時';
COMMENT ON COLUMN payments.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN payments.updated_at IS '更新日時';
COMMENT ON COLUMN payments.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN payments.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN payments.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_payments_company_period
  ON payments(company_id, payroll_period_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_payments
BEFORE UPDATE ON payments
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE payment_lines (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  payment_id uuid NOT NULL REFERENCES payments(id),
  payslip_id uuid NOT NULL REFERENCES payslips(id),
  amount numeric(12,2) NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE payment_lines IS '支払バッチ（行）';
COMMENT ON COLUMN payment_lines.company_id IS '会社ID';
COMMENT ON COLUMN payment_lines.payment_id IS '支払バッチID';
COMMENT ON COLUMN payment_lines.payslip_id IS '給与明細ID';
COMMENT ON COLUMN payment_lines.amount IS '支払金額';

COMMENT ON COLUMN payment_lines.created_at IS '作成日時';
COMMENT ON COLUMN payment_lines.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN payment_lines.updated_at IS '更新日時';
COMMENT ON COLUMN payment_lines.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN payment_lines.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN payment_lines.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_payment_lines_company_payment
  ON payment_lines(company_id, payment_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_payment_lines
BEFORE UPDATE ON payment_lines
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- =========================================================
-- 7) 監査ログ / ジョブログ（RDSに保持）
-- =========================================================

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  actor_user_id uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  action text NOT NULL,
  target_type text NOT NULL,
  target_id uuid NOT NULL,

  before_data jsonb,
  after_data jsonb,
  request_id text,
  happened_at timestamptz NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE audit_logs IS '監査ログ（業務データ変更の追跡）';
COMMENT ON COLUMN audit_logs.company_id IS '会社ID';
COMMENT ON COLUMN audit_logs.actor_user_id IS '実行者ユーザーID';
COMMENT ON COLUMN audit_logs.action IS '操作';
COMMENT ON COLUMN audit_logs.target_type IS '対象テーブル名（users|employees等）';
COMMENT ON COLUMN audit_logs.target_id IS '対象ID';
COMMENT ON COLUMN audit_logs.before_data IS '変更前データ';
COMMENT ON COLUMN audit_logs.after_data IS '変更後データ';
COMMENT ON COLUMN audit_logs.request_id IS 'リクエストID';
COMMENT ON COLUMN audit_logs.happened_at IS '発生日時';

COMMENT ON COLUMN audit_logs.created_at IS '作成日時';
COMMENT ON COLUMN audit_logs.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN audit_logs.updated_at IS '更新日時';
COMMENT ON COLUMN audit_logs.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN audit_logs.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN audit_logs.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_audit_logs_company_time
  ON audit_logs(company_id, happened_at);

CREATE TRIGGER set_updated_at_audit_logs
BEFORE UPDATE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE job_runs (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  job_type text NOT NULL, -- payroll_calculation|payroll_finalize|export_csv|import_csv|etc
  status text NOT NULL DEFAULT 'running', -- running|success|failed|canceled
  request_id text,

  started_at timestamptz NOT NULL,
  finished_at timestamptz,

  params jsonb,
  result jsonb,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE job_runs IS 'ジョブ実行（業務バッチの履歴）';
COMMENT ON COLUMN job_runs.company_id IS '会社ID';
COMMENT ON COLUMN job_runs.job_type IS 'ジョブ種別';
COMMENT ON COLUMN job_runs.status IS '状態（running|success|failed|canceled）';
COMMENT ON COLUMN job_runs.request_id IS 'リクエストID';
COMMENT ON COLUMN job_runs.started_at IS '開始日時';
COMMENT ON COLUMN job_runs.finished_at IS '終了日時';
COMMENT ON COLUMN job_runs.params IS 'パラメータ';
COMMENT ON COLUMN job_runs.result IS '結果';

COMMENT ON COLUMN job_runs.created_at IS '作成日時';
COMMENT ON COLUMN job_runs.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN job_runs.updated_at IS '更新日時';
COMMENT ON COLUMN job_runs.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN job_runs.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN job_runs.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_job_runs_company_time
  ON job_runs(company_id, started_at);

CREATE TRIGGER set_updated_at_job_runs
BEFORE UPDATE ON job_runs
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


CREATE TABLE job_run_events (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES companies(id),

  job_run_id uuid NOT NULL REFERENCES job_runs(id),
  level text NOT NULL, -- info|warn|error
  message text NOT NULL,
  metadata jsonb,
  happened_at timestamptz NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE job_run_events IS 'ジョブ実行ログ（業務バッチの詳細イベント）';
COMMENT ON COLUMN job_run_events.company_id IS '会社ID';
COMMENT ON COLUMN job_run_events.job_run_id IS 'ジョブ実行ID';
COMMENT ON COLUMN job_run_events.level IS 'レベル';
COMMENT ON COLUMN job_run_events.message IS 'メッセージ';
COMMENT ON COLUMN job_run_events.metadata IS 'メタデータ';
COMMENT ON COLUMN job_run_events.happened_at IS '発生日時';

COMMENT ON COLUMN job_run_events.created_at IS '作成日時';
COMMENT ON COLUMN job_run_events.created_by IS '作成者ユーザーID';
COMMENT ON COLUMN job_run_events.updated_at IS '更新日時';
COMMENT ON COLUMN job_run_events.updated_by IS '更新者ユーザーID';
COMMENT ON COLUMN job_run_events.deleted_at IS '削除日時（ソフト削除）';
COMMENT ON COLUMN job_run_events.deleted_by IS '削除者ユーザーID';

CREATE INDEX ix_job_run_events_company_job
  ON job_run_events(company_id, job_run_id, happened_at);

CREATE TRIGGER set_updated_at_job_run_events
BEFORE UPDATE ON job_run_events
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- =========================================================
-- 補足（重要）:
-- created_by/updated_by を NOT NULL + FK にしているため、
-- 初期データ投入では "systemユーザー" を最初に作る運用が必要です。
-- DEFERRABLE INITIALLY DEFERRED を付けてあるので、同一トランザクションで
-- 自己参照（created_by = 自分）も成立させやすくしています。
-- =========================================================
