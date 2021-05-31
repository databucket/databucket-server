
CREATE OR REPLACE FUNCTION before_delete() RETURNS trigger AS '
DECLARE
    this_query text;
BEGIN
    EXECUTE format(''DELETE FROM "%s-h" WHERE data_id = %s'', TG_TABLE_NAME, OLD.data_id);
    RETURN OLD;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION after_insert() RETURNS trigger AS '
DECLARE
    this_query text;
BEGIN
    EXECUTE format(''INSERT INTO "%s-h" (data_id, tag_id, reserved, properties, modified_by) VALUES (%s, %L::integer, %L::boolean, %L, %L)'', TG_TABLE_NAME, NEW.data_id, NEW.tag_id, NEW.reserved, NEW.properties, NEW.created_by);
    RETURN NULL;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION after_update() RETURNS trigger AS '
DECLARE
    this_query text;
	insert_changes boolean;
	tagId integer;
	reserved boolean;
	properties text;
BEGIN
	insert_changes := false;
    tagId := null;
	IF ((OLD.tag_id is null) != (NEW.tag_id is null)) OR (OLD.tag_id != NEW.tag_id)  THEN
		tagId := NEW.tag_id;
        insert_changes := true;
    END IF;

    reserved = null;
    IF OLD.reserved != NEW.reserved THEN
		reserved := NEW.reserved;
        insert_changes := true;
    END IF;

    properties := null;
    IF (OLD.properties != NEW.properties) THEN
		properties := NEW.properties;
		insert_changes := true;
    END IF;

    IF insert_changes = true THEN
		EXECUTE format(''INSERT INTO "%s-h" (data_id, tag_id, reserved, properties, modified_by) VALUES (%s, %L::integer, %L::boolean, %L::jsonb, %L)'', TG_TABLE_NAME, NEW.data_id, tagId, reserved, properties, NEW.modified_by);
    END IF;

    RETURN NULL;
END;
' LANGUAGE plpgsql;
