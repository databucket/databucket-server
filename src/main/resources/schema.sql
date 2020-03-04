CREATE TABLE IF NOT EXISTS `_group` (
  `group_id` int(5) NOT NULL AUTO_INCREMENT,
  `group_name` varchar(50) NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `buckets` json DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `group_id_UNIQUE` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_class` (
  `class_id` int(5) NOT NULL AUTO_INCREMENT,
  `class_name` varchar(50) NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`class_id`),
  UNIQUE KEY `class_id_UNIQUE` (`class_id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_bucket` (
  `bucket_id` int(5) NOT NULL AUTO_INCREMENT,
  `bucket_name` varchar(50) NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `index` INT(5) NOT NULL DEFAULT 100,
  `class_id` int(5) NULL,
  `icon_name` varchar(30) DEFAULT NULL,
  `history` bit(1) NOT NULL DEFAULT b'0',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`bucket_id`),
  UNIQUE KEY `bucket_id_UNIQUE` (`bucket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_tag` (
  `tag_id` int(5) NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) NOT NULL,
  `bucket_id` int(5) NULL,
  `class_id` int(5) NULL,
  `description` varchar(100) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `tag_id_UNIQUE` (`tag_id`),
  KEY `fk_tag_bucket_id_idx` (`bucket_id`),
  CONSTRAINT `fk_tag_bucket_id` FOREIGN KEY (`bucket_id`) REFERENCES `_bucket` (`bucket_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_columns` (
  `columns_id` int(11) NOT NULL AUTO_INCREMENT,
  `columns_name` varchar(50) NOT NULL,
  `bucket_id` int(5) NULL,
  `class_id` int(5) NULL,
  `description` varchar(100) CHARACTER SET utf8 DEFAULT NULL,
  `columns` json NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`columns_id`),
  UNIQUE KEY `columns_id_UNIQUE` (`columns_id`),
  KEY `fk_columns_bucket_id_idx` (`bucket_id`),
  CONSTRAINT `fk_columns_bucket_id` FOREIGN KEY (`bucket_id`) REFERENCES `_bucket` (`bucket_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_filter` (
  `filter_id` int(11) NOT NULL AUTO_INCREMENT,
  `filter_name` varchar(50) NOT NULL,
  `bucket_id` int(5) NULL,
  `class_id` int(5) NULL,
  `conditions` json NOT NULL,
  `description` varchar(100) CHARACTER SET utf8 DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`filter_id`),
  UNIQUE KEY `filter_id_UNIQUE` (`filter_id`),
  KEY `fk_filter_bucket_id_idx` (`bucket_id`),
  CONSTRAINT `fk_filter_bucket_id` FOREIGN KEY (`bucket_id`) REFERENCES `_bucket` (`bucket_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_task` (
  `task_id` int(11) NOT NULL AUTO_INCREMENT,
  `task_name` varchar(50) NOT NULL,
  `bucket_id` int(5) NULL,
  `class_id` int(5) NULL,
  `configuration` json NOT NULL,
  `description` varchar(100) CHARACTER SET utf8 DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `task_id_UNIQUE` (`task_id`),
  KEY `fk_task_bucket_id_idx` (`task_id`),
  CONSTRAINT `fk_task_bucket_id` FOREIGN KEY (`bucket_id`) REFERENCES `_bucket` (`bucket_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_event` (
  `event_id` int(11) NOT NULL AUTO_INCREMENT,
  `event_name` varchar(50) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'0',
  `bucket_id` int(5) NULL,
  `class_id` int(5) NULL,
  `schedule` json NOT NULL,
  `tasks` json NOT NULL,
  `description` varchar(100) CHARACTER SET utf8 DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`event_id`),
  UNIQUE KEY `event_id_UNIQUE` (`event_id`),
  KEY `fk_event_bucket_id_idx` (`event_id`),
  CONSTRAINT `fk_event_bucket_id` FOREIGN KEY (`bucket_id`) REFERENCES `_bucket` (`bucket_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_event_log` (
  `event_log_id` int(11) NOT NULL AUTO_INCREMENT,
  `event_id` int(5) NOT NULL,
  `task_id` int(5) NOT NULL,
  `bucket_id` int(5) NOT NULL,
  `affected` int(5) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_log_id`),
  UNIQUE KEY `event_log_id_UNIQUE` (`event_log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `_view` (
  `view_id` int(11) NOT NULL AUTO_INCREMENT,
  `view_name` varchar(50) NOT NULL,
  `bucket_id` int(5) NULL,
  `class_id` int(5) NULL,
  `description` varchar(100) CHARACTER SET utf8 DEFAULT NULL,
  `filter_id` int(11) NULL,
  `columns_id` int(5) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(50) NOT NULL,
  `updated_at` TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` varchar(50) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`view_id`),
  UNIQUE KEY `view_id_UNIQUE` (`view_id`),
  KEY `fk_view_bucket_id_idx` (`bucket_id`),
  CONSTRAINT `fk_view_bucket_id` FOREIGN KEY (`bucket_id`) REFERENCES `_bucket` (`bucket_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
