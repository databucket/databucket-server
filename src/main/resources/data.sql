SET sql_mode="NO_AUTO_VALUE_ON_ZERO";

SET @bundleId = '{ "title": "Id", "field": "bundle_id", "type": "numeric", "editable": "never", "sorting": true, "filtering": true },';
SET @tagName = '{ "title": "Tag", "field": "tag_id", "type": "numeric", "editable": "always", "sorting": true, "filtering": true },';
SET @bundleLocked = '{ "title": "Locked", "field": "locked", "type": "boolean", "editable": "always", "sorting": true, "filtering": true },';
SET @lockedBy = '{ "title": "Locked by", "field": "locked_by", "type": "string", "editable": "never", "sorting": true, "filtering": true },';
SET @createdAt = '{ "title": "Created by", "field": "created_by", "type": "string", "editable": "never", "sorting": true, "filtering": true },';
SET @createdBy = '{ "title": "Created at", "field": "created_at", "type": "datetime", "editable": "never", "sorting": true, "filtering": true },';
SET @updatedAt = '{ "title": "Updated by", "field": "updated_by", "type": "string", "editable": "never", "sorting": true, "filtering": true },';
SET @updatedBy = '{ "title": "Updated at", "field": "updated_at", "type": "datetime", "editable": "never", "sorting": true, "filtering": true }';
SET @concatColumns = CONCAT('[', @bundleId, @tagName, @bundleLocked, @lockedBy, @createdAt, @createdBy, @updatedAt, @updatedBy, ']');

INSERT IGNORE INTO `_columns` (`columns_id`, `columns_name`, `columns`, `created_by`) VALUES (0, "default columns", @concatColumns, CURRENT_USER());
INSERT IGNORE INTO `_tag` (`tag_id`, `tag_name`, `created_by`) VALUES (0, "default tag", CURRENT_USER());
INSERT IGNORE INTO `_view` (`view_id`, `view_name`, columns_id, `created_by`) VALUES (0, "default view", 0, CURRENT_USER());