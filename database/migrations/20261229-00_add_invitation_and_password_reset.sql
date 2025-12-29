-- =========================================================
-- 20261229-00_add_invitation_and_password_reset.sql
-- 招待トークンとパスワードリセット機能のテーブル追加
-- =========================================================

-- クライアントエンコーディングをUTF-8に設定
SET client_encoding TO 'UTF8';

-- =========================================================
-- m_companies に max_users カラムを追加（簡易ライセンス数管理）
-- =========================================================

ALTER TABLE m_companies
  ADD COLUMN max_users int;

COMMENT ON COLUMN m_companies.max_users IS '最大ユーザー数（NULLの場合は無制限）';

-- =========================================================
-- t_user_invitations（ユーザー招待）
-- =========================================================

CREATE TABLE t_user_invitations (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  company_id uuid NOT NULL REFERENCES m_companies(id),

  -- 招待情報
  email text NOT NULL,
  token text NOT NULL,
  role text NOT NULL, -- system_admin|admin|manager|employee

  -- 従業員情報（必須）
  employee_no text NOT NULL,
  employment_type text, -- fulltime|parttime|contract
  hire_date date,

  -- トークン制御
  expires_at timestamptz NOT NULL,
  max_uses int NOT NULL DEFAULT 1,
  used_count int NOT NULL DEFAULT 0,
  used_at timestamptz,
  used_by uuid REFERENCES m_users(id),

  -- ステータス
  status text NOT NULL DEFAULT 'pending', -- pending|used|expired|canceled

  -- 監査列
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid NOT NULL REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED,
  deleted_at timestamptz,
  deleted_by uuid REFERENCES m_users(id) DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE t_user_invitations IS 'ユーザー招待';
COMMENT ON COLUMN t_user_invitations.id IS '招待ID';
COMMENT ON COLUMN t_user_invitations.company_id IS '会社ID';
COMMENT ON COLUMN t_user_invitations.email IS '招待先メールアドレス';
COMMENT ON COLUMN t_user_invitations.token IS '招待トークン（UUID v7推奨）';
COMMENT ON COLUMN t_user_invitations.role IS '付与するロール';
COMMENT ON COLUMN t_user_invitations.employee_no IS '社員番号（必須）';
COMMENT ON COLUMN t_user_invitations.employment_type IS '雇用区分';
COMMENT ON COLUMN t_user_invitations.hire_date IS '入社日';
COMMENT ON COLUMN t_user_invitations.expires_at IS '有効期限';
COMMENT ON COLUMN t_user_invitations.max_uses IS '最大使用回数';
COMMENT ON COLUMN t_user_invitations.used_count IS '使用回数';
COMMENT ON COLUMN t_user_invitations.used_at IS '使用日時';
COMMENT ON COLUMN t_user_invitations.used_by IS '使用したユーザーID';
COMMENT ON COLUMN t_user_invitations.status IS 'ステータス（pending|used|expired|canceled）';

CREATE UNIQUE INDEX uq_t_user_invitations_token
  ON t_user_invitations(token)
  WHERE deleted_at IS NULL AND status = 'pending';

CREATE INDEX ix_t_user_invitations_company_email
  ON t_user_invitations(company_id, email)
  WHERE deleted_at IS NULL;

CREATE INDEX ix_t_user_invitations_company_status
  ON t_user_invitations(company_id, status)
  WHERE deleted_at IS NULL;

CREATE TRIGGER set_updated_at_t_user_invitations
BEFORE UPDATE ON t_user_invitations
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- =========================================================
-- t_password_reset_tokens（パスワードリセットトークン）
-- =========================================================

CREATE TABLE t_password_reset_tokens (
  id uuid PRIMARY KEY DEFAULT gen_uuid_v7(),
  user_id uuid NOT NULL REFERENCES m_users(id),
  token text NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  used_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE t_password_reset_tokens IS 'パスワードリセットトークン';
COMMENT ON COLUMN t_password_reset_tokens.id IS 'トークンID';
COMMENT ON COLUMN t_password_reset_tokens.user_id IS 'ユーザーID';
COMMENT ON COLUMN t_password_reset_tokens.token IS 'リセットトークン（UUID v7推奨）';
COMMENT ON COLUMN t_password_reset_tokens.expires_at IS '有効期限';
COMMENT ON COLUMN t_password_reset_tokens.used_at IS '使用日時';
COMMENT ON COLUMN t_password_reset_tokens.created_at IS '作成日時';

CREATE INDEX ix_t_password_reset_tokens_user_id
  ON t_password_reset_tokens(user_id)
  WHERE used_at IS NULL;

CREATE INDEX ix_t_password_reset_tokens_token
  ON t_password_reset_tokens(token)
  WHERE used_at IS NULL;

