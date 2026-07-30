CREATE TABLE IF NOT EXISTS `interaction_event` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '交互事件主键',
  `session_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属体验会话编号',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型',
  `source_module` VARCHAR(20) DEFAULT NULL COMMENT '来源模块',
  `target_module` VARCHAR(20) DEFAULT NULL COMMENT '目标模块',
  `source_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源业务ID',
  `target_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '目标业务ID',
  `payload` JSON DEFAULT NULL COMMENT '事件附加信息',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件产生时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_session_time` (`session_id`, `created_at`),
  KEY `idx_event_type_time` (`event_type`, `created_at`)
) ENGINE=InnoDB COMMENT='用户交互事件日志';
