-- =========================================================
-- データベース削除スクリプト
-- 重要: このスクリプトは postgres データベースに接続してから実行してください
--       現在接続しているデータベース（auratime）を削除することはできません
-- =========================================================
-- 使用方法:
--   1. 直接実行する場合（重要: -d postgres を指定）:
--      psql -U postgres -d postgres -f database/migrations/20251228_drop_database.sql
--
--   2. docker-compose を使用している場合:
--      【PowerShellの場合】
--      Get-Content database/migrations/20251228_drop_database.sql | docker exec -i auratime-postgres psql -U postgres -d postgres
--      または
--      docker-compose exec postgres psql -U postgres -d postgres -f /docker-entrypoint-initdb.d/20251228_drop_database.sql
--
--      【Bash/CMDの場合】
--      docker exec -i auratime-postgres psql -U postgres -d postgres < database/migrations/20251228_drop_database.sql
-- =========================================================

-- =========================================================
-- 重要: 接続確認
-- =========================================================
-- このスクリプトを実行する前に、必ず postgres データベースに接続してください
-- 現在接続しているデータベースが auratime の場合は、このスクリプトを実行できません
--
-- A5:SQL Mk-2 で実行する場合:
--   1. 接続を切断
--   2. 新しい接続を作成し、データベース名に "postgres" を指定
--   3. その接続でこのスクリプトを実行
-- =========================================================

-- 既存の接続を強制終了してからデータベースを削除
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'auratime'
  AND pid <> pg_backend_pid();

-- データベースを削除
DROP DATABASE IF EXISTS auratime;

-- データベースを再作成
CREATE DATABASE auratime
  WITH
  OWNER = postgres
  ENCODING = 'UTF8'
  LC_COLLATE = 'C'
  LC_CTYPE = 'C'
  TEMPLATE = template0;

COMMENT ON DATABASE auratime IS 'AuraTime勤怠管理システムのデータベース';

