-- Upgrade of a Databucket database used by 3.6.x (or older) to 3.7.0.
--
-- 3.7.0 requires unique user emails (single sign-on matches users by email). On a fresh database
-- the unique constraint is created automatically, an existing database needs this script:
--
--   psql -h <host> -U <user> -d <database> -f upgrade/3.7.0.sql
--
-- It runs in one transaction and can be run more than once. When several users share an email
-- it stops with the list of them and changes nothing: give each user a unique email (or clear it)
-- and run it again.

\set ON_ERROR_STOP on

BEGIN;

-- 1. Emails are stored trimmed and lower-cased, a blank one as null (as 3.7.0 saves them).
UPDATE users
SET email = NULLIF(lower(trim(email)), '')
WHERE email IS DISTINCT FROM NULLIF(lower(trim(email)), '');

-- 2. Stop when several users share an email.
DO $$
DECLARE
    conflicts text;
BEGIN
    SELECT string_agg(format('  %s: %s', email, user_names), E'\n' ORDER BY email)
    INTO conflicts
    FROM (SELECT email, string_agg(user_name, ', ' ORDER BY user_name) AS user_names
          FROM users
          WHERE email IS NOT NULL
          GROUP BY email
          HAVING count(*) > 1) duplicates;

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION E'Several users share an email. Give each of them a unique email (or clear it) and run the script again:\n%', conflicts;
    END IF;
END $$;

-- 3. Unique email, unless the database already has it.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM pg_index i
                            JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = i.indkey[0]
                   WHERE i.indrelid = 'users'::regclass
                     AND i.indisunique
                     AND i.indnkeyatts = 1
                     AND a.attname = 'email') THEN
        ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email);
        RAISE NOTICE 'Added the unique constraint users_email_key';
    ELSE
        RAISE NOTICE 'Emails are already unique';
    END IF;
END $$;

COMMIT;
