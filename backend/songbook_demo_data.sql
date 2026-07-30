USE `guzheng_experience_rebuild`;

START TRANSACTION;

-- 推荐词与反馈词
INSERT INTO descriptor (descriptor_type, name, enabled)
VALUES
    ('STYLE', '古风', TRUE),
    ('STYLE', '传统', TRUE),
    ('MOOD', '舒缓', TRUE),
    ('MOOD', '典雅', TRUE),
    ('MOOD', '激昂', TRUE),
    ('SCENE', '静心', TRUE),
    ('SCENE', '展示', TRUE),
    ('FEEDBACK', '好听', TRUE),
    ('FEEDBACK', '震撼', TRUE)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

SET @desc_gufeng = (
    SELECT id FROM descriptor WHERE descriptor_type = 'STYLE' AND name = '古风'
);
SET @desc_traditional = (
    SELECT id FROM descriptor WHERE descriptor_type = 'STYLE' AND name = '传统'
);
SET @desc_relaxed = (
    SELECT id FROM descriptor WHERE descriptor_type = 'MOOD' AND name = '舒缓'
);
SET @desc_elegant = (
    SELECT id FROM descriptor WHERE descriptor_type = 'MOOD' AND name = '典雅'
);
SET @desc_powerful = (
    SELECT id FROM descriptor WHERE descriptor_type = 'MOOD' AND name = '激昂'
);
SET @desc_meditation = (
    SELECT id FROM descriptor WHERE descriptor_type = 'SCENE' AND name = '静心'
);
SET @desc_show = (
    SELECT id FROM descriptor WHERE descriptor_type = 'SCENE' AND name = '展示'
);

-- 渔舟唱晚
INSERT INTO playable_work (work_kind, title, playable_status)
SELECT 'SONG', '渔舟唱晚', 'READY'
WHERE NOT EXISTS (
    SELECT 1 FROM playable_work WHERE work_kind = 'SONG' AND title = '渔舟唱晚'
);
SET @song_yuzhou = (
    SELECT id FROM playable_work
    WHERE work_kind = 'SONG' AND title = '渔舟唱晚'
    ORDER BY id LIMIT 1
);

INSERT INTO song (
    work_id, artist_name, origin_period, background_text, style_text, featured_excerpt
) VALUES (
    @song_yuzhou,
    '娄树华',
    '近现代',
    '描绘夕阳西下、渔舟归航的湖面景色。',
    '旋律优美，意境悠远。',
    '适合展示慢板主题段。'
)
ON DUPLICATE KEY UPDATE
    artist_name = VALUES(artist_name),
    origin_period = VALUES(origin_period),
    background_text = VALUES(background_text),
    style_text = VALUES(style_text),
    featured_excerpt = VALUES(featured_excerpt);

INSERT IGNORE INTO song_alias (song_id, alias_text, alias_kind)
VALUES
    (@song_yuzhou, '渔舟', 'TITLE'),
    (@song_yuzhou, '娄树华', 'ARTIST');

INSERT INTO song_descriptor (song_id, descriptor_id, weight, basis)
VALUES
    (@song_yuzhou, @desc_gufeng, 0.9000, 'CURATED'),
    (@song_yuzhou, @desc_relaxed, 0.9500, 'CURATED'),
    (@song_yuzhou, @desc_meditation, 0.8500, 'CURATED')
ON DUPLICATE KEY UPDATE
    weight = VALUES(weight),
    basis = VALUES(basis);

-- 高山流水
INSERT INTO playable_work (work_kind, title, playable_status)
SELECT 'SONG', '高山流水', 'READY'
WHERE NOT EXISTS (
    SELECT 1 FROM playable_work WHERE work_kind = 'SONG' AND title = '高山流水'
);
SET @song_gaoshan = (
    SELECT id FROM playable_work
    WHERE work_kind = 'SONG' AND title = '高山流水'
    ORDER BY id LIMIT 1
);

INSERT INTO song (
    work_id, artist_name, origin_period, background_text, style_text, featured_excerpt
) VALUES (
    @song_gaoshan,
    '古曲',
    '古代',
    '以高山流水寄托知音相遇的文化意象。',
    '旋律典雅，层次丰富。',
    '适合展示主题变奏片段。'
)
ON DUPLICATE KEY UPDATE
    artist_name = VALUES(artist_name),
    origin_period = VALUES(origin_period),
    background_text = VALUES(background_text),
    style_text = VALUES(style_text),
    featured_excerpt = VALUES(featured_excerpt);

INSERT IGNORE INTO song_alias (song_id, alias_text, alias_kind)
VALUES
    (@song_gaoshan, '流水知音', 'OTHER'),
    (@song_gaoshan, '古曲', 'ARTIST');

INSERT INTO song_descriptor (song_id, descriptor_id, weight, basis)
VALUES
    (@song_gaoshan, @desc_gufeng, 0.9500, 'CURATED'),
    (@song_gaoshan, @desc_traditional, 0.9500, 'CURATED'),
    (@song_gaoshan, @desc_elegant, 0.9000, 'CURATED'),
    (@song_gaoshan, @desc_meditation, 0.7500, 'CURATED')
ON DUPLICATE KEY UPDATE
    weight = VALUES(weight),
    basis = VALUES(basis);

-- 战台风
INSERT INTO playable_work (work_kind, title, playable_status)
SELECT 'SONG', '战台风', 'READY'
WHERE NOT EXISTS (
    SELECT 1 FROM playable_work WHERE work_kind = 'SONG' AND title = '战台风'
);
SET @song_typhoon = (
    SELECT id FROM playable_work
    WHERE work_kind = 'SONG' AND title = '战台风'
    ORDER BY id LIMIT 1
);

INSERT INTO song (
    work_id, artist_name, origin_period, background_text, style_text, featured_excerpt
) VALUES (
    @song_typhoon,
    '王昌元',
    '现代',
    '表现码头工人与台风搏斗的热烈场景。',
    '节奏强烈，技巧性和表现力突出。',
    '适合展示快速指序与扫摇片段。'
)
ON DUPLICATE KEY UPDATE
    artist_name = VALUES(artist_name),
    origin_period = VALUES(origin_period),
    background_text = VALUES(background_text),
    style_text = VALUES(style_text),
    featured_excerpt = VALUES(featured_excerpt);

INSERT IGNORE INTO song_alias (song_id, alias_text, alias_kind)
VALUES
    (@song_typhoon, '台风', 'TITLE'),
    (@song_typhoon, '王昌元', 'ARTIST');

INSERT INTO song_descriptor (song_id, descriptor_id, weight, basis)
VALUES
    (@song_typhoon, @desc_traditional, 0.8000, 'CURATED'),
    (@song_typhoon, @desc_powerful, 0.9800, 'CURATED'),
    (@song_typhoon, @desc_show, 0.9500, 'CURATED')
ON DUPLICATE KEY UPDATE
    weight = VALUES(weight),
    basis = VALUES(basis);

-- 演示资源地址；接入真实文件后替换 storage_uri。
INSERT INTO digital_asset (
    asset_kind, storage_uri, mime_type, checksum_sha256
) VALUES (
    'IMAGE',
    '/assets/song/yuzhou-cover.jpg',
    'image/jpeg',
    'demo-yuzhou-cover-000000000000000000000000000000000000000000000'
)
ON DUPLICATE KEY UPDATE mime_type = VALUES(mime_type);
SET @asset_yuzhou_cover = (
    SELECT id FROM digital_asset WHERE storage_uri = '/assets/song/yuzhou-cover.jpg'
);

INSERT INTO digital_asset (
    asset_kind, storage_uri, mime_type, checksum_sha256, duration_ms
) VALUES (
    'AUDIO',
    '/assets/song/yuzhou-preview.mp3',
    'audio/mpeg',
    'demo-yuzhou-audio-000000000000000000000000000000000000000000000',
    30000
)
ON DUPLICATE KEY UPDATE
    mime_type = VALUES(mime_type),
    duration_ms = VALUES(duration_ms);
SET @asset_yuzhou_preview = (
    SELECT id FROM digital_asset WHERE storage_uri = '/assets/song/yuzhou-preview.mp3'
);

INSERT INTO digital_asset (
    asset_kind, storage_uri, mime_type, checksum_sha256
) VALUES (
    'SCORE',
    '/assets/song/yuzhou-score.json',
    'application/json',
    'demo-yuzhou-score-000000000000000000000000000000000000000000000'
)
ON DUPLICATE KEY UPDATE mime_type = VALUES(mime_type);
SET @asset_yuzhou_score = (
    SELECT id FROM digital_asset WHERE storage_uri = '/assets/song/yuzhou-score.json'
);

INSERT INTO song_resource (song_id, asset_id, resource_role, display_order)
VALUES
    (@song_yuzhou, @asset_yuzhou_cover, 'COVER', 1),
    (@song_yuzhou, @asset_yuzhou_preview, 'PREVIEW', 1),
    (@song_yuzhou, @asset_yuzhou_score, 'SCORE', 1)
ON DUPLICATE KEY UPDATE display_order = VALUES(display_order);

COMMIT;
