INSERT INTO columns (columns_id, columns_name, created_by, columns)
    VALUES (0, 'default columns', CURRENT_USER,
    '[
        { "title": "Id", "field": "data_id", "type": "numeric", "editable": "never", "sorting": true, "filtering": true },
        { "title": "Tag", "field": "tag_id", "type": "numeric", "editable": "always", "sorting": true, "filtering": true },
        { "title": "Locked", "field": "locked", "type": "boolean", "editable": "always", "sorting": true, "filtering": true },
        { "title": "Locked by", "field": "locked_by", "type": "string", "editable": "never", "sorting": true, "filtering": true },
        { "title": "Created by", "field": "created_by", "type": "string", "editable": "never", "sorting": true, "filtering": true },
        { "title": "Created at", "field": "created_at", "type": "datetime", "editable": "never", "sorting": true, "filtering": true },
        { "title": "Updated by", "field": "updated_by", "type": "string", "editable": "never", "sorting": true, "filtering": true },
        { "title": "Updated at", "field": "updated_at", "type": "datetime", "editable": "never", "sorting": true, "filtering": true }
    ]')
    ON CONFLICT DO NOTHING;

INSERT INTO views (view_id, view_name, columns_id, created_by)
    VALUES (0, 'default view', 0, CURRENT_USER)
    ON CONFLICT DO NOTHING;
