-- CREATE TABLE IF NOT EXISTS public."groups" (
--     group_id serial NOT NULL,
--     group_name character varying(50) NOT NULL,
--     description character varying(500),
--     buckets jsonb,
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (group_id)
-- );

-- CREATE TABLE IF NOT EXISTS public."dataclasses" (
--     class_id serial NOT NULL,
--     class_name character varying(50) NOT NULL,
--     description character varying(500),
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (class_id)
-- );

-- CREATE TABLE IF NOT EXISTS public."buckets" (
--     bucket_id serial NOT NULL,
--     bucket_name character varying(50) NOT NULL,
--     description character varying(500),
--     index smallint NOT NULL DEFAULT 10,
--     class_id smallint,
--     icon_name character varying(30),
--     history boolean NOT NULL DEFAULT false,
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (bucket_id),
--     CONSTRAINT fk_bucket_class_id
--         FOREIGN KEY(class_id)
--             REFERENCES dataclasses(class_id)
-- );

-- CREATE TABLE IF NOT EXISTS public."tags" (
--     tag_id serial NOT NULL,
--     tag_name character varying(50) NOT NULL,
--     bucket_id smallint DEFAULT NULL,
--     class_id smallint DEFAULT NULL,
--     description character varying(500),
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (tag_id),
--     CONSTRAINT fk_tag_bucket_id
--         FOREIGN KEY(bucket_id)
--             REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_tag_class_id
--         FOREIGN KEY(class_id)
--             REFERENCES dataclasses(class_id)
-- );

-- CREATE TABLE IF NOT EXISTS public."columns" (
--     columns_id serial NOT NULL,
--     columns_name character varying(50) NOT NULL,
--     bucket_id smallint DEFAULT NULL,
--     class_id smallint DEFAULT NULL,
--     description character varying(500),
--     columns jsonb NOT NULL,
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (columns_id),
--     CONSTRAINT fk_columns_bucket_id
--         FOREIGN KEY(bucket_id)
--             REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_columns_class_id
--         FOREIGN KEY(class_id)
--             REFERENCES dataclasses(class_id)
--     );


-- CREATE TABLE IF NOT EXISTS public."filters" (
--     filter_id serial NOT NULL,
--     filter_name character varying(50) NOT NULL,
--     bucket_id smallint DEFAULT NULL,
--     class_id smallint DEFAULT NULL,
--     conditions jsonb NOT NULL,
--     description character varying(500),
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (filter_id),
--     CONSTRAINT fk_filter_bucket_id
--         FOREIGN KEY(bucket_id)
--             REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_filter_class_id
--         FOREIGN KEY(class_id)
--             REFERENCES dataclasses(class_id)
--     );


-- CREATE TABLE IF NOT EXISTS public."tasks" (
--     task_id serial NOT NULL,
--     task_name character varying(50) NOT NULL,
--     bucket_id smallint DEFAULT NULL,
--     class_id smallint DEFAULT NULL,
--     configuration jsonb NOT NULL,
--     description character varying(500),
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (task_id),
--     CONSTRAINT fk_task_bucket_id
--         FOREIGN KEY(bucket_id)
--             REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_task_class_id
--         FOREIGN KEY(class_id)
--             REFERENCES dataclasses(class_id)
--     );

-- CREATE TABLE IF NOT EXISTS public."events" (
--     event_id serial NOT NULL,
--     event_name character varying(50) NOT NULL,
--     active boolean NOT NULL DEFAULT false,
--     bucket_id smallint DEFAULT NULL,
--     class_id smallint DEFAULT NULL,
--     schedule jsonb NOT NULL,
--     tasks jsonb NOT NULL,
--     description character varying(500),
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (event_id),
--     CONSTRAINT fk_event_bucket_id
--       FOREIGN KEY(bucket_id)
--           REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_event_class_id
--       FOREIGN KEY(class_id)
--           REFERENCES dataclasses(class_id)
-- );

-- CREATE TABLE IF NOT EXISTS public."views" (
--     view_id serial NOT NULL,
--     view_name character varying(50) NOT NULL,
--     bucket_id smallint DEFAULT NULL,
--     class_id smallint DEFAULT NULL,
--     description character varying(500),
--     filter_id smallint DEFAULT NULL,
--     columns_id smallint NOT NULL,
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     created_by character varying(50) NOT NULL,
--     updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     updated_by character varying(50),
--     deleted boolean NOT NULL DEFAULT false,
--     PRIMARY KEY (view_id),
--     CONSTRAINT fk_view_bucket_id
--       FOREIGN KEY(bucket_id)
--           REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_view_class_id
--       FOREIGN KEY(class_id)
--           REFERENCES dataclasses(class_id)
-- );

-- CREATE TABLE IF NOT EXISTS public."events_log" (
--     event_log_id serial NOT NULL,
--     event_id smallint NOT NULL,
--     task_id smallint NOT NULL,
--     bucket_id smallint NOT NULL,
--     affected integer NOT NULL,
--     created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
--     PRIMARY KEY (event_log_id),
--     CONSTRAINT fk_event_log_bucket_id
--        FOREIGN KEY(bucket_id)
--            REFERENCES buckets(bucket_id),
--     CONSTRAINT fk_event_log_event_id
--        FOREIGN KEY(event_id)
--            REFERENCES events(event_id),
--     CONSTRAINT fk_event_log_task_id
--         FOREIGN KEY(task_id)
--             REFERENCES tasks(task_id)
-- );

CREATE OR REPLACE FUNCTION before_delete() RETURNS trigger AS '
DECLARE
    this_query text;
BEGIN
    EXECUTE format(''DELETE FROM "%s_h" WHERE data_id = %s'', TG_TABLE_NAME, OLD.data_id);
    RETURN OLD;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION after_insert() RETURNS trigger AS '
DECLARE
    this_query text;
BEGIN
    EXECUTE format(''INSERT INTO "%s_" (data_id, tag_id, locked, properties, updated_by) VALUES (%s, %L::integer, %L::boolean, %L, %L)'', TG_TABLE_NAME, NEW.data_id, NEW.tag_id, NEW.locked, NEW.properties, NEW.created_by);
    RETURN NULL;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION after_update() RETURNS trigger AS '
DECLARE
    this_query text;
	insert_changes boolean;
	tagId integer;
	locked boolean;
	properties text;
BEGIN
	insert_changes := false;
    tagId := null;
	IF ((OLD.tag_id is null) != (NEW.tag_id is null)) OR (OLD.tag_id != NEW.tag_id)  THEN
		tagId := NEW.tag_id;
        insert_changes := true;
    END IF;

    locked = null;
    IF OLD.locked != NEW.locked THEN
		locked := NEW.locked;
        insert_changes := true;
    END IF;

    properties := null;
    IF (OLD.properties != NEW.properties) THEN
		properties := NEW.properties;
		insert_changes := true;
    END IF;

    IF insert_changes = true THEN
		EXECUTE format(''INSERT INTO "%s_h" (data_id, tag_id, locked, properties, updated_by) VALUES (%s, %L::integer, %L::boolean, %L::jsonb, %L)'', TG_TABLE_NAME, NEW.data_id, tagId, locked, properties, NEW.updated_by);
    END IF;

    RETURN NULL;
END;
' LANGUAGE plpgsql;
