INSERT INTO playable_work (
    work_kind, title, playable_status
) VALUES (
    'SONG', '接口测试曲目·渔舟唱晚', 'READY'
);
SET @songbook_test_song_1 = LAST_INSERT_ID();

INSERT INTO song (
    work_id, artist_name, origin_period, background_text, style_text, featured_excerpt
) VALUES (
    @songbook_test_song_1,
    '接口测试作者甲',
    '近现代',
    '用于我要点歌接口集成测试的曲目背景。',
    '旋律舒缓，具有古风意境。',
    '慢板主题段'
);

INSERT INTO song_alias (song_id, alias_text, alias_kind)
VALUES (@songbook_test_song_1, '接口测试渔舟', 'TITLE');

INSERT INTO playable_work (
    work_kind, title, playable_status
) VALUES (
    'SONG', '接口测试曲目·高山流水', 'READY'
);
SET @songbook_test_song_2 = LAST_INSERT_ID();

INSERT INTO song (
    work_id, artist_name, origin_period, background_text, style_text, featured_excerpt
) VALUES (
    @songbook_test_song_2,
    '接口测试作者乙',
    '古代',
    '用于替代推荐和分页验证的测试曲目。',
    '典雅、悠远。',
    '主题片段'
);

INSERT INTO descriptor (descriptor_type, name, enabled)
VALUES
    ('STYLE', '接口测试古风', TRUE),
    ('MOOD', '接口测试舒缓', TRUE),
    ('SCENE', '接口测试静心', TRUE),
    ('FEEDBACK', '接口测试好听', TRUE);

SET @songbook_test_style = (
    SELECT id FROM descriptor
    WHERE descriptor_type = 'STYLE' AND name = '接口测试古风'
);
SET @songbook_test_mood = (
    SELECT id FROM descriptor
    WHERE descriptor_type = 'MOOD' AND name = '接口测试舒缓'
);
SET @songbook_test_scene = (
    SELECT id FROM descriptor
    WHERE descriptor_type = 'SCENE' AND name = '接口测试静心'
);

INSERT INTO song_descriptor (song_id, descriptor_id, weight, basis)
VALUES
    (@songbook_test_song_1, @songbook_test_style, 0.9000, 'CURATED'),
    (@songbook_test_song_1, @songbook_test_mood, 0.9500, 'CURATED'),
    (@songbook_test_song_1, @songbook_test_scene, 0.8000, 'CURATED'),
    (@songbook_test_song_2, @songbook_test_style, 0.8500, 'CURATED');

INSERT INTO digital_asset (
    asset_kind, storage_uri, mime_type, checksum_sha256
) VALUES (
    'IMAGE',
    '/test/songbook/cover.jpg',
    'image/jpeg',
    'songbook-test-cover-checksum'
);
SET @songbook_test_cover = LAST_INSERT_ID();

INSERT INTO digital_asset (
    asset_kind, storage_uri, mime_type, checksum_sha256, duration_ms
) VALUES (
    'AUDIO',
    '/test/songbook/preview.mp3',
    'audio/mpeg',
    'songbook-test-preview-checksum',
    30000
);
SET @songbook_test_preview = LAST_INSERT_ID();

INSERT INTO digital_asset (
    asset_kind, storage_uri, mime_type, checksum_sha256
) VALUES (
    'SCORE',
    '/test/songbook/score.json',
    'application/json',
    'songbook-test-score-checksum'
);
SET @songbook_test_score = LAST_INSERT_ID();

INSERT INTO song_resource (song_id, asset_id, resource_role, display_order)
VALUES
    (@songbook_test_song_1, @songbook_test_cover, 'COVER', 1),
    (@songbook_test_song_1, @songbook_test_preview, 'PREVIEW', 1),
    (@songbook_test_song_1, @songbook_test_score, 'SCORE', 1);

