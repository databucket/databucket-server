-- Upgrade of a Databucket database used by 3.21.0 - 3.22.x to 3.23.0.
--
-- From 3.23.0 a job (Settings > Jobs, formerly Schedules) may have no cron: it then runs only by its
-- keyword or by Run. The column task_schedules.cron was created NOT NULL; this script drops that
-- constraint (the new keyword column is added at startup). Without it, saving a job without a cron fails.
--
--   psql -h <host> -U <user> -d <database> -f upgrade/3.23.0.sql
--
-- It runs in one transaction and can be run more than once (also before 3.21.0 created the table: then
-- it changes nothing).

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name = 'task_schedules'
                 AND column_name = 'cron'
                 AND is_nullable = 'NO') THEN
        ALTER TABLE public.task_schedules ALTER COLUMN cron DROP NOT NULL;
        RAISE NOTICE 'task_schedules.cron: NOT NULL dropped';
    ELSE
        RAISE NOTICE 'task_schedules.cron: nothing to change';
    END IF;
END $$;

COMMIT;
