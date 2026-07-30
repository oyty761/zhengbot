package com.guzheng.songbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
@Rollback
@Sql(
        scripts = "/songbook-test-data.sql",
        config = @SqlConfig(encoding = "UTF-8")
)
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class SongbookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void completeSongbookFlowUsesRebuiltDatabaseSchema() throws Exception {
        Long songId = jdbcTemplate.queryForObject(
                "SELECT @songbook_test_song_1",
                Long.class);

        mockMvc.perform(get("/api/songbook/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modules.length()").value(2));

        mockMvc.perform(get("/api/songbook/songs")
                        .param("page", "1")
                        .param("size", "12")
                        .param("keyword", "接口测试渔舟"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].songId").value(songId));

        mockMvc.perform(get("/api/songbook/songs/{songId}", songId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.coverUrl").value("/test/songbook/cover.jpg"))
                .andExpect(jsonPath("$.data.previewUrl").value("/test/songbook/preview.mp3"))
                .andExpect(jsonPath("$.data.scoreUrl").value("/test/songbook/score.json"));

        mockMvc.perform(post("/api/songbook/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "接口测试渔舟",
                                  "inputChannel": "TEXT",
                                  "limit": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requestKind").value("SEARCH"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.songs[0].songId").value(songId));

        mockMvc.perform(post("/api/songbook/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "我想听接口测试古风、接口测试舒缓的曲子",
                                  "inputChannel": "TEXT",
                                  "limit": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requestKind").value("RECOMMEND"))
                .andExpect(jsonPath("$.data.songs[0].songId").value(songId))
                .andExpect(jsonPath("$.data.songs[0].matchScore").value(0.925));

        mockMvc.perform(post("/api/songbook/alternatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "数据库中不存在的测试曲目",
                                  "inputChannel": "TEXT",
                                  "limit": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requestKind").value("ALTERNATIVE"))
                .andExpect(jsonPath("$.data.songs.length()").value(2));

        MvcResult performanceResult = mockMvc.perform(post("/api/songbook/performances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "songId": %d,
                                  "sessionToken": "550e8400-e29b-41d4-a716-446655440000"
                                }
                                """.formatted(songId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.runStatus").value("QUEUED"))
                .andReturn();

        JsonNode performanceJson = objectMapper.readTree(
                performanceResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long performanceId = performanceJson.path("data").path("performanceId").asLong();

        mockMvc.perform(get("/api/songbook/performances/{performanceId}", performanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.songId").value(songId))
                .andExpect(jsonPath("$.data.runStatus").value("QUEUED"));

        jdbcTemplate.update("""
                UPDATE performance_run
                SET run_status = 'SUCCEEDED',
                    started_at = CURRENT_TIMESTAMP(3),
                    ended_at = CURRENT_TIMESTAMP(3)
                WHERE id = ?
                """, performanceId);
        // 同一测试事务中 MyBatis 会保留一级缓存；模拟机器人调度器更新后需清理缓存。
        sqlSessionTemplate.clearCache();

        Long feedbackDescriptorId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM descriptor
                WHERE descriptor_type = 'FEEDBACK'
                  AND name = '接口测试好听'
                """,
                Long.class);

        mockMvc.perform(get("/api/songbook/feedback/descriptors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.descriptorId == %d)]".formatted(
                        feedbackDescriptorId)).exists());

        mockMvc.perform(post("/api/songbook/performances/{performanceId}/feedback", performanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "comment": "旋律很舒缓",
                                  "descriptorIds": [%d]
                                }
                                """.formatted(feedbackDescriptorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.performanceId").value(performanceId))
                .andExpect(jsonPath("$.data.message").value("反馈已记录"));
    }

    @Test
    void demoDataScriptIsValidAndIdempotent() throws Exception {
        String sql = Files.readString(
                        Path.of("songbook_demo_data.sql"),
                        StandardCharsets.UTF_8)
                .replace("START TRANSACTION;", "")
                .replace("COMMIT;", "");
        EncodedResource resource = new EncodedResource(
                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);

        ScriptUtils.executeSqlScript(
                DataSourceUtils.getConnection(dataSource),
                resource);
        ScriptUtils.executeSqlScript(
                DataSourceUtils.getConnection(dataSource),
                resource);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM playable_work
                WHERE work_kind = 'SONG'
                  AND title IN ('渔舟唱晚', '高山流水', '战台风')
                """,
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(3, count);
    }
}

