-- 乐器探秘板块测试数据
-- 先执行 guzheng_experience_jpt.sql 创建表后再执行本文件

USE `guzheng_experience_jpt`;

-- 清空相关表（避免重复插入时报主键冲突）
SET FOREIGN_KEY_CHECKS = 0;
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
-- 琴弦音频存放在 src/main/resources/static/assets/audio/guzheng/，
-- Spring Boot 对外访问地址为 /assets/audio/guzheng/{文件名}。
INSERT INTO `digital_asset` (`id`, `asset_kind`, `storage_uri`, `mime_type`, `checksum_sha256`, `duration_ms`, `created_at`) VALUES
(1, 'MODEL', 'https://example.com/assets/guzheng_model.glb', 'model/gltf-binary', NULL, NULL, NOW(3)),
(2, 'IMAGE', 'https://example.com/assets/string.png', 'image/png', NULL, NULL, NOW(3)),
(3, 'IMAGE', 'https://example.com/assets/bridge.png', 'image/png', NULL, NULL, NOW(3)),
(4, 'IMAGE', 'https://example.com/assets/soundboard.png', 'image/png', NULL, NULL, NOW(3)),
(5,  'AUDIO', '/assets/audio/guzheng/string_01_D6.mp3',  'audio/mpeg', '9e5de717eef5e0f017a23ba150c56edcc4ab1b6d341715ff6d5a961158ef51df', 2534, NOW(3)),
(6,  'AUDIO', '/assets/audio/guzheng/string_02_B5.mp3',  'audio/mpeg', '6047cfb14f8382594301f51afa81c20c0f23f5771c690fa380a05dc8e7efef9a', 2926, NOW(3)),
(7,  'AUDIO', '/assets/audio/guzheng/string_03_A5.mp3',  'audio/mpeg', '2d0825192a16286f1398ced841f3419ec844376d7ebeec0acd59cd510341b064', 2612, NOW(3)),
(8,  'AUDIO', '/assets/audio/guzheng/string_04_Fs5.mp3', 'audio/mpeg', 'eb9b362852876b7568a865fd43b35848db77673a1dc2a3deb4df4116cd0985b9', 3291, NOW(3)),
(9,  'AUDIO', '/assets/audio/guzheng/string_05_E5.mp3',  'audio/mpeg', 'a015b57945e27e74c5e5dd3e6f1e354b54939f8234f16d31293c64ef3ea10315', 2220, NOW(3)),
(10, 'AUDIO', '/assets/audio/guzheng/string_06_D5.mp3',  'audio/mpeg', 'a0d7a264bda0e8632820ad0727e7cacf191f2f2c8c6452f27d6c42c6165890bf', 3030, NOW(3)),
(11, 'AUDIO', '/assets/audio/guzheng/string_07_B4.mp3',  'audio/mpeg', '0a00056a9ac3e1549699104dcc62df477bb409268469d70bb6904926941f465e', 1881, NOW(3)),
(12, 'AUDIO', '/assets/audio/guzheng/string_08_A4.mp3',  'audio/mpeg', '7ef66f6e957546548f606adfef5afdebc35efdea68aeb92ee7b85085121f6896', 3056, NOW(3)),
(13, 'AUDIO', '/assets/audio/guzheng/string_09_Fs4.mp3', 'audio/mpeg', 'f6c3561f01ed56b831b7423a59cb6361877febc334ab47239cbeeaf9df1eb4cd', 3135, NOW(3)),
(14, 'AUDIO', '/assets/audio/guzheng/string_10_E4.mp3',  'audio/mpeg', '4376ed7f52e8c294dccdb741c7d7ae7e4b45740df642c1182c93c983c48082de', 3422, NOW(3)),
(15, 'AUDIO', '/assets/audio/guzheng/string_11_D4.mp3',  'audio/mpeg', '57056d7da4338a22a5d4b06a28e544c463260f49f71a819e70876e74bea5ef7f', 2873, NOW(3)),
(16, 'AUDIO', '/assets/audio/guzheng/string_12_B3.mp3',  'audio/mpeg', 'b3098a3b84a14d38f41dd6c97c695760f912473160a5b59782ef006a50e0baa1', 3500, NOW(3)),
(17, 'AUDIO', '/assets/audio/guzheng/string_13_A3.mp3',  'audio/mpeg', 'b8cae7ff81f80bad74a7aa2b50eb16bd52af4f622f3265b74634a6ed3651984c', 3370, NOW(3)),
(18, 'AUDIO', '/assets/audio/guzheng/string_14_Fs3.mp3', 'audio/mpeg', '9542835273532a427363113a9c9b0ba6dff369fc72dcc77f382d6af3f80ff3d1', 4493, NOW(3)),
(19, 'AUDIO', '/assets/audio/guzheng/string_15_E3.mp3',  'audio/mpeg', 'd6cb98b6c2c4be8da95e89b8c1e9fe2ada184009947a492d652fad612337db52', 3814, NOW(3)),
(20, 'AUDIO', '/assets/audio/guzheng/string_16_D3.mp3',  'audio/mpeg', '2f2e208d6872fa84a3e3e4381a5c2f325dd4954941571f4b471fcae19e90d4c4', 4702, NOW(3)),
(21, 'AUDIO', '/assets/audio/guzheng/string_17_B2.mp3',  'audio/mpeg', '4fbcca717a84e4b52db87aeffb0f75cc39a6b8eee163adb797ad83d9d15f485b', 4598, NOW(3)),
(22, 'AUDIO', '/assets/audio/guzheng/string_18_A2.mp3',  'audio/mpeg', 'c900ccff48a8ad9c58210729b4b8b0b0bbb0184348063aeae0f5e9daccd8bade', 4127, NOW(3)),
(23, 'AUDIO', '/assets/audio/guzheng/string_19_Fs2.mp3', 'audio/mpeg', '3b5f26bf26242131d109bc699c813d5ae0f8c5e69df964ee789b0181a7edd7b8', 4885, NOW(3)),
(24, 'AUDIO', '/assets/audio/guzheng/string_20_E2.mp3',  'audio/mpeg', 'a1e9e050a31bbf59cac31380c0f261a17eaeb27c508a7d2e95bc47348382e5ea', 2691, NOW(3)),
(25, 'AUDIO', '/assets/audio/guzheng/string_21_D2.mp3',  'audio/mpeg', 'b7b8d8358d2eafe2c2d182bb84e9dd08d94b1833cf9becf2e547b2ac7cc14b49', 4049, NOW(3));

-- 2. 乐器部件（普通部件）
INSERT INTO `instrument_part` (`id`, `part_code`, `part_kind`, `name`, `summary`, `function_text`, `position_text`, `performance_relation`, `display_order`, `enabled`) VALUES
(1, 'STRING', 'COMPONENT', '琴弦', '古筝发声核心部件', '振动发声，决定音高', '纵向排列于面板，通过岳山固定', '按弦位置改变音色，弦的松紧度决定音准', 1, TRUE),
(2, 'BRIDGE', 'COMPONENT', '琴码', '支撑琴弦并传递振动', '支撑琴弦，将振动传递到共鸣箱', '位于琴弦与面板之间', '琴码材质和位置影响音色和音量', 2, TRUE),
(3, 'SOUNDBOARD', 'COMPONENT', '面板', '古筝共鸣箱的重要组成部分', '接收并放大琴弦振动', '位于古筝正面', '面板材质决定古筝音色特点', 3, TRUE);

-- 3. 乐器部件（21 根古筝弦，与 JPT D-pentatonic 调弦完整对应）
INSERT INTO `instrument_part` (`id`, `part_code`, `part_kind`, `name`, `summary`, `function_text`, `position_text`, `performance_relation`, `display_order`, `enabled`) VALUES
(4,  'STRING_01', 'STRING', '第1弦',  '古筝第1弦',  '振动发声', '第1弦位置',  'D6',  101, TRUE),
(5,  'STRING_02', 'STRING', '第2弦',  '古筝第2弦',  '振动发声', '第2弦位置',  'B5',  102, TRUE),
(6,  'STRING_03', 'STRING', '第3弦',  '古筝第3弦',  '振动发声', '第3弦位置',  'A5',  103, TRUE),
(7,  'STRING_04', 'STRING', '第4弦',  '古筝第4弦',  '振动发声', '第4弦位置',  'F#5', 104, TRUE),
(8,  'STRING_05', 'STRING', '第5弦',  '古筝第5弦',  '振动发声', '第5弦位置',  'E5',  105, TRUE),
(9,  'STRING_06', 'STRING', '第6弦',  '古筝第6弦',  '振动发声', '第6弦位置',  'D5',  106, TRUE),
(10, 'STRING_07', 'STRING', '第7弦',  '古筝第7弦',  '振动发声', '第7弦位置',  'B4',  107, TRUE),
(11, 'STRING_08', 'STRING', '第8弦',  '古筝第8弦',  '振动发声', '第8弦位置',  'A4',  108, TRUE),
(12, 'STRING_09', 'STRING', '第9弦',  '古筝第9弦',  '振动发声', '第9弦位置',  'F#4', 109, TRUE),
(13, 'STRING_10', 'STRING', '第10弦', '古筝第10弦', '振动发声', '第10弦位置', 'E4',  110, TRUE),
(14, 'STRING_11', 'STRING', '第11弦', '古筝第11弦', '振动发声', '第11弦位置', 'D4',  111, TRUE),
(15, 'STRING_12', 'STRING', '第12弦', '古筝第12弦', '振动发声', '第12弦位置', 'B3',  112, TRUE),
(16, 'STRING_13', 'STRING', '第13弦', '古筝第13弦', '振动发声', '第13弦位置', 'A3',  113, TRUE),
(17, 'STRING_14', 'STRING', '第14弦', '古筝第14弦', '振动发声', '第14弦位置', 'F#3', 114, TRUE),
(18, 'STRING_15', 'STRING', '第15弦', '古筝第15弦', '振动发声', '第15弦位置', 'E3',  115, TRUE),
(19, 'STRING_16', 'STRING', '第16弦', '古筝第16弦', '振动发声', '第16弦位置', 'D3',  116, TRUE),
(20, 'STRING_17', 'STRING', '第17弦', '古筝第17弦', '振动发声', '第17弦位置', 'B2',  117, TRUE),
(21, 'STRING_18', 'STRING', '第18弦', '古筝第18弦', '振动发声', '第18弦位置', 'A2',  118, TRUE),
(22, 'STRING_19', 'STRING', '第19弦', '古筝第19弦', '振动发声', '第19弦位置', 'F#2', 119, TRUE),
(23, 'STRING_20', 'STRING', '第20弦', '古筝第20弦', '振动发声', '第20弦位置', 'E2',  120, TRUE),
(24, 'STRING_21', 'STRING', '第21弦', '古筝第21弦', '振动发声', '第21弦位置', 'D2',  121, TRUE);

-- 4. 部件资源关联
INSERT INTO `part_resource` (`part_id`, `asset_id`, `resource_role`, `display_order`) VALUES
(1, 2, 'DETAIL_IMAGE', 1),
(2, 3, 'DETAIL_IMAGE', 1),
(3, 4, 'DETAIL_IMAGE', 1),
(4,  5,  'DEMO_AUDIO', 1),
(5,  6,  'DEMO_AUDIO', 1),
(6,  7,  'DEMO_AUDIO', 1),
(7,  8,  'DEMO_AUDIO', 1),
(8,  9,  'DEMO_AUDIO', 1),
(9,  10, 'DEMO_AUDIO', 1),
(10, 11, 'DEMO_AUDIO', 1),
(11, 12, 'DEMO_AUDIO', 1),
(12, 13, 'DEMO_AUDIO', 1),
(13, 14, 'DEMO_AUDIO', 1),
(14, 15, 'DEMO_AUDIO', 1),
(15, 16, 'DEMO_AUDIO', 1),
(16, 17, 'DEMO_AUDIO', 1),
(17, 18, 'DEMO_AUDIO', 1),
(18, 19, 'DEMO_AUDIO', 1),
(19, 20, 'DEMO_AUDIO', 1),
(20, 21, 'DEMO_AUDIO', 1),
(21, 22, 'DEMO_AUDIO', 1),
(22, 23, 'DEMO_AUDIO', 1),
(23, 24, 'DEMO_AUDIO', 1),
(24, 25, 'DEMO_AUDIO', 1);

-- 5. 琴弦音高配置
-- 与 JPT 1.0 的 D-pentatonic 21 弦映射完全一致
INSERT INTO `string_profile` (`part_id`, `string_no`, `midi_note`, `register_name`) VALUES
(4, 1, 86, '高音区'),
(5, 2, 83, '高音区'),
(6, 3, 81, '高音区'),
(7, 4, 78, '高音区'),
(8, 5, 76, '高音区'),
(9, 6, 74, '中音区'),
(10, 7, 71, '中音区'),
(11, 8, 69, '中音区'),
(12, 9, 66, '中音区'),
(13, 10, 64, '中音区'),
(14, 11, 62, '中音区'),
(15, 12, 59, '中音区'),
(16, 13, 57, '中音区'),
(17, 14, 54, '中音区'),
(18, 15, 52, '中音区'),
(19, 16, 50, '低音区'),
(20, 17, 47, '低音区'),
(21, 18, 45, '低音区'),
(22, 19, 42, '低音区'),
(23, 20, 40, '低音区'),
(24, 21, 38, '低音区');

-- 6. 历史时期
INSERT INTO `history_entry` (`id`, `parent_id`, `entry_kind`, `title`, `time_label`, `start_year`, `end_year`, `content`, `cover_asset_id`, `display_order`, `enabled`) VALUES
(1, NULL, 'PERIOD', '先秦起源', '公元前221年前', -221, NULL, '古筝雏形源于秦地，是古代丝弦乐器的重要代表。', 2, 1, TRUE),
(2, NULL, 'PERIOD', '唐宋发展', '618-1279年', 618, 1279, '形制定型，成为宫廷雅乐和民间音乐的重要乐器。', 2, 2, TRUE),
(3, NULL, 'PERIOD', '近现代传播', '20世纪至今', 1900, NULL, '古筝艺术不断创新发展，走向世界舞台。', 2, 3, TRUE);

-- 7. 可演奏作品
INSERT INTO `playable_work` (`id`, `work_kind`, `title`, `playable_status`, `created_at`, `updated_at`) VALUES
(1, 'SONG', '高山流水', 'READY', NOW(3), NOW(3)),
(2, 'SONG', '渔舟唱晚', 'READY', NOW(3), NOW(3));

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
