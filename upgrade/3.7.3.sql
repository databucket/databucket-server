-- Upgrade of a Databucket database used by 3.7.2 (or older) to 3.7.3.
--
-- 3.7.3 creates indexes on the tables of new buckets and stores the tag of a history entry as bigint
-- (as the data table and the tags table do). Buckets created before need this script:
--
--   psql -h <host> -U <user> -d <database> -f upgrade/3.7.3.sql
--
-- For each bucket it:
--   - indexes the data table on (tag_id, reserved): filtering by tag and reserving data,
--   - indexes the history table on data_id: the history of a row, and removing data,
--   - changes tag_id of the history table from smallint to bigint.
-- Changing the type rewrites each history table and locks it meanwhile, so run the script while
-- Databucket is stopped. It runs in one transaction and can be run more than once.

\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    data_table text;
    history_table text;
    buckets integer := 0;
BEGIN
    FOR data_table IN SELECT tablename
                      FROM pg_tables
                      WHERE schemaname = 'public'
                        AND tablename ~ '^x-bucket-[0-9]+$'
                      ORDER BY tablename
    LOOP
        buckets := buckets + 1;
        IF to_regclass(format('public.%I', data_table || '_tag_id_reserved_idx')) IS NULL THEN
            EXECUTE format('CREATE INDEX %I ON public.%I (tag_id, reserved)',
                           data_table || '_tag_id_reserved_idx', data_table);
        END IF;

        history_table := data_table || '-h';
        IF to_regclass(format('public.%I', history_table)) IS NOT NULL THEN
            IF EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_schema = 'public'
                         AND table_name = history_table
                         AND column_name = 'tag_id'
                         AND data_type <> 'bigint') THEN
                EXECUTE format('ALTER TABLE public.%I ALTER COLUMN tag_id TYPE bigint', history_table);
            END IF;
            IF to_regclass(format('public.%I', history_table || '_data_id_idx')) IS NULL THEN
                EXECUTE format('CREATE INDEX %I ON public.%I (data_id)',
                               history_table || '_data_id_idx', history_table);
            END IF;
        END IF;
    END LOOP;

    RAISE NOTICE 'Buckets upgraded: %', buckets;
END $$;

COMMIT;
