-- =========================================================
-- 初期データ投入
-- =========================================================

-- クライアントエンコーディングをUTF-8に設定
SET client_encoding TO 'UTF8';

-- 初期データ投入（既に存在する場合はスキップ）
DO $$
DECLARE
  v_user_id uuid;
  v_company_id uuid;
  v_password_hash text;
  v_existing_user_id uuid;
  v_existing_company_id uuid;
BEGIN
  -- 既存データのチェック
  SELECT id INTO v_existing_user_id FROM m_users WHERE email = 'iq87io25@gmail.com' AND deleted_at IS NULL LIMIT 1;
  SELECT id INTO v_existing_company_id FROM m_companies WHERE code = 'IQ187' AND deleted_at IS NULL LIMIT 1;

  -- 既にデータが存在する場合はスキップ
  IF v_existing_user_id IS NOT NULL AND v_existing_company_id IS NOT NULL THEN
    RETURN;
  END IF;

  -- BCryptハッシュを生成（pgcryptoのcrypt関数を使用）
  v_password_hash := crypt('Password123!', gen_salt('bf', 10));

  -- 1. ユーザーを作成（既に存在しない場合のみ）
  IF v_existing_user_id IS NULL THEN
    INSERT INTO m_users (
      id,
      email,
      password_hash,
      family_name,
      first_name,
      family_name_kana,
      first_name_kana,
      status,
      created_at,
      created_by,
      updated_at,
      updated_by
    ) VALUES (
      gen_uuid_v7(),
      'iq87io25@gmail.com',
      v_password_hash,
      '米沢',
      '正寛',
      'ヨネザワ',
      'マサヒロ',
      'active',
      now(),
      gen_uuid_v7(), -- 一時的なUUID（後で更新）
      now(),
      gen_uuid_v7()  -- 一時的なUUID（後で更新）
    )
    RETURNING id INTO v_user_id;

    -- created_by/updated_byを自分自身に更新（DEFERRABLE制約により可能）
    UPDATE m_users
    SET created_by = v_user_id,
        updated_by = v_user_id
    WHERE id = v_user_id;
  ELSE
    v_user_id := v_existing_user_id;
  END IF;

  -- 2. 会社を作成（既に存在しない場合のみ）
  IF v_existing_company_id IS NULL THEN
    INSERT INTO m_companies (
      id,
      name,
      code,
      timezone,
      currency,
      created_at,
      created_by,
      updated_at,
      updated_by
    ) VALUES (
      gen_uuid_v7(),
      '株式会社IQ187',
      'IQ187',
      'Asia/Tokyo',
      'JPY',
      now(),
      v_user_id,
      now(),
      v_user_id
    )
    RETURNING id INTO v_company_id;
  ELSE
    v_company_id := v_existing_company_id;
  END IF;

  -- 3. 会社所属（system_adminロール）を作成（既に存在しない場合のみ）
  IF NOT EXISTS (
    SELECT 1 FROM r_company_memberships
    WHERE company_id = v_company_id
      AND user_id = v_user_id
      AND deleted_at IS NULL
  ) THEN
    INSERT INTO r_company_memberships (
      id,
      company_id,
      user_id,
      role,
      joined_at,
      created_at,
      created_by,
      updated_at,
      updated_by
    ) VALUES (
      gen_uuid_v7(),
      v_company_id,
      v_user_id,
      'system_admin',
      now(),
      now(),
      v_user_id,
      now(),
      v_user_id
    );
  END IF;

END $$;

