-- 乐器探秘板块测试数据
-- 先执行 guzheng_experience_rebuild.sql 创建表后再执行本文件

USE `guzheng_experience_rebuild`;

-- 清空相关表（避免重复插入时报主键冲突）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `interaction_event`;
TRUNCATE TABLE `history_song`;
TRUNCATE TABLE `song`;
TRUNCATE TABLE `playable_work`;
TRUNCATE TABLE `history_entry`;
TRUNCATE TABLE `string_profile`;
TRUNCATE TABLE `part_resource`;
TRUNCATE TABLE `part_marker`;
TRUNCATE TABLE `instrument_part`;
TRUNCATE TABLE `experience_session`;
TRUNCATE TABLE `digital_asset`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 数字资源
INSERT INTO `digital_asset` (`id`, `asset_kind`, `storage_uri`, `mime_type`, `duration_ms`, `created_at`) VALUES
(1, 'MODEL', 'https://example.com/assets/guzheng_model.glb', 'model/gltf-binary', NULL, NOW(3)),
(2, 'IMAGE', 'https://example.com/assets/string.png', 'image/png', NULL, NOW(3)),
(3, 'IMAGE', 'https://example.com/assets/bridge.png', 'image/png', NULL, NOW(3)),
(4, 'IMAGE', 'https://example.com/assets/soundboard.png', 'image/png', NULL, NOW(3)),
(5, 'AUDIO', 'https://example.com/audio/string_5.mp3', 'audio/mpeg', 3000, NOW(3)),
(6, 'AUDIO', 'https://example.com/audio/string_1.mp3', 'audio/mpeg', 3000, NOW(3));

-- 2. 乐器部件（普通部件）
INSERT INTO `instrument_part` (`id`, `part_code`, `part_kind`, `name`, `summary`, `function_text`, `position_text`, `performance_relation`, `display_order`, `enabled`) VALUES
(1, 'STRING', 'COMPONENT', '琴弦', '古筝发声核心部件', '振动发声，决定音高', '纵向排列于面板，通过岳山固定', '按弦位置改变音色，弦的松紧度决定音准', 1, TRUE),
(2, 'BRIDGE', 'COMPONENT', '琴码', '支撑琴弦并传递振动', '支撑琴弦，将振动传递到共鸣箱', '位于琴弦与面板之间', '琴码材质和位置影响音色和音量', 2, TRUE),
(3, 'SOUNDBOARD', 'COMPONENT', '面板', '古筝共鸣箱的重要组成部分', '接收并放大琴弦振动', '位于古筝正面', '面板材质决定古筝音色特点', 3, TRUE);

-- 3. 乐器部件（琴弦，21根古筝弦）
-- 这里只插入第1弦和第5弦作为示例
INSERT INTO `instrument_part` (`id`, `part_code`, `part_kind`, `name`, `summary`, `function_text`, `position_text`, `performance_relation`, `display_order`, `enabled`) VALUES
(4, 'STRING_01', 'STRING', '第1弦', '古筝最高音弦', '振动发声', '最靠近演奏者右侧', '高音区', 101, TRUE),
(5, 'STRING_05', 'STRING', '第5弦', '古筝中音区琴弦', '振动发声', '面板中部', '中音区', 105, TRUE);

-- 4. 部件资源关联
INSERT INTO `part_resource` (`part_id`, `asset_id`, `resource_role`, `display_order`) VALUES
(1, 2, 'DETAIL_IMAGE', 1),
(2, 3, 'DETAIL_IMAGE', 1),
(3, 4, 'DETAIL_IMAGE', 1),
(4, 6, 'DEMO_AUDIO', 1),
(5, 5, 'DEMO_AUDIO', 1);

-- 5. 琴弦音高配置
-- MIDI音高参考：第1弦 D6=86（21弦古筝常见定弦），第5弦 B5=83
INSERT INTO `string_profile` (`part_id`, `string_no`, `midi_note`, `register_name`) VALUES
(4, 1, 86, '高音区'),
(5, 5, 83, '中音区');

-- 6. 历史时期
INSERT INTO `history_entry` (`id`, `parent_id`, `entry_kind`, `title`, `time_label`, `start_year`, `end_year`, `content`, `cover_asset_id`, `display_order`, `enabled`) VALUES
(1, NULL, 'PERIOD', '先秦起源', '公元前221年前', -221, NULL, '古筝雏形源于秦地，是古代丝弦乐器的重要代表。', 2, 1, TRUE),
(2, NULL, 'PERIOD', '唐宋发展', '618-1279年', 618, 1279, '形制定型，成为宫廷雅乐和民间音乐的重要乐器。', 2, 2, TRUE),
(3, NULL, 'PERIOD', '近现代传播', '20世纪至今', 1900, NULL, '古筝艺术不断创新发展，走向世界舞台。', 2, 3, TRUE);

-- 7. 可演奏作品
INSERT INTO `playable_work` (`id`, `work_kind`, `title`, `playable_status`, `created_at`, `updated_at`) VALUES
(1, 'SONG', '《高山流水》', 'READY', NOW(3), NOW(3)),
(2, 'SONG', '《渔舟唱晚》', 'READY', NOW(3), NOW(3));

-- 8. 歌曲信息
INSERT INTO `song` (`work_id`, `artist_name`, `origin_period`, `background_text`, `style_text`, `featured_excerpt`) VALUES
(1, '古曲', '先秦', '古筝名曲，喻知音难觅。', '典雅流畅', '伯牙子期，高山流水遇知音'),
(2, '古曲', '唐宋', '描绘渔村晚景。', '宁静优美', '夕阳西下，渔舟唱晚归');

-- 9. 历史阶段与歌曲关联
INSERT INTO `history_song` (`history_entry_id`, `song_id`, `display_order`) VALUES
(1, 1, 1),
(1, 2, 2),
(2, 2, 1);

-- 10. 体验会话（用于 song-link 接口测试）
INSERT INTO `experience_session` (`id`, `session_token`, `started_at`, `ended_at`) VALUES
(1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(3), NULL);
