-- JPT 1.0 基础调弦数据
-- 请先执行 guzheng_experience_jpt.sql，再执行本文件。

USE `guzheng_experience_jpt`;

START TRANSACTION;

INSERT INTO `jpt_tuning` (
  `tuning_code`, `mapping_version`, `name`, `string_count`, `description`, `enabled`
) VALUES (
  'D-pentatonic',
  '1.0',
  'D 五声音阶 21 弦标准调弦',
  21,
  'JPT 1.0 默认调弦；弦号从高音到低音按 1 至 21 编号。',
  TRUE
) AS `new_tuning`
ON DUPLICATE KEY UPDATE
  `name` = `new_tuning`.`name`,
  `string_count` = `new_tuning`.`string_count`,
  `description` = `new_tuning`.`description`,
  `enabled` = `new_tuning`.`enabled`;

SET @jpt_d_pentatonic_id = (
  SELECT `id`
  FROM `jpt_tuning`
  WHERE `tuning_code` = 'D-pentatonic'
    AND `mapping_version` = '1.0'
);

INSERT INTO `jpt_tuning_string` (`tuning_id`, `string_no`, `pitch_name`, `midi_note`) VALUES
(@jpt_d_pentatonic_id,  1, 'D6',  86),
(@jpt_d_pentatonic_id,  2, 'B5',  83),
(@jpt_d_pentatonic_id,  3, 'A5',  81),
(@jpt_d_pentatonic_id,  4, 'F#5', 78),
(@jpt_d_pentatonic_id,  5, 'E5',  76),
(@jpt_d_pentatonic_id,  6, 'D5',  74),
(@jpt_d_pentatonic_id,  7, 'B4',  71),
(@jpt_d_pentatonic_id,  8, 'A4',  69),
(@jpt_d_pentatonic_id,  9, 'F#4', 66),
(@jpt_d_pentatonic_id, 10, 'E4',  64),
(@jpt_d_pentatonic_id, 11, 'D4',  62),
(@jpt_d_pentatonic_id, 12, 'B3',  59),
(@jpt_d_pentatonic_id, 13, 'A3',  57),
(@jpt_d_pentatonic_id, 14, 'F#3', 54),
(@jpt_d_pentatonic_id, 15, 'E3',  52),
(@jpt_d_pentatonic_id, 16, 'D3',  50),
(@jpt_d_pentatonic_id, 17, 'B2',  47),
(@jpt_d_pentatonic_id, 18, 'A2',  45),
(@jpt_d_pentatonic_id, 19, 'F#2', 42),
(@jpt_d_pentatonic_id, 20, 'E2',  40),
(@jpt_d_pentatonic_id, 21, 'D2',  38) AS `new_string`
ON DUPLICATE KEY UPDATE
  `pitch_name` = `new_string`.`pitch_name`,
  `midi_note` = `new_string`.`midi_note`;

COMMIT;
