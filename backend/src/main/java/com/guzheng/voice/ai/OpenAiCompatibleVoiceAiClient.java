package com.guzheng.voice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guzheng.common.BusinessException;
import com.guzheng.voice.config.VoiceApiProperties;
import com.guzheng.voice.model.VoiceRecords;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** OpenAI-compatible HTTP 实现，供应商参数全部来自本地私密配置。 */
@Slf4j
@Component
public class OpenAiCompatibleVoiceAiClient implements VoiceAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final VoiceApiProperties properties;

    public OpenAiCompatibleVoiceAiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            VoiceApiProperties properties) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Transcription transcribe(
            byte[] audio,
            String fileName,
            String mimeType,
            String language) {
        if (!StringUtils.hasText(properties.asrUrl())) {
            throw new BusinessException(503, "ASR 服务尚未配置");
        }

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new NamedByteArrayResource(audio, fileName))
                .contentType(MediaType.parseMediaType(mimeType));
        body.part("model", properties.asrModel());
        body.part("language", normalizeLanguage(language));
        body.part("response_format", "verbose_json");

        try {
            JsonNode response = restClient.post()
                    .uri(properties.asrUrl())
                    .headers(headers -> authorize(headers, properties.asrApiKey()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(JsonNode.class);
            String content = firstText(response, "/text", "/data/text", "/result/text");
            BigDecimal confidence = firstDecimal(
                    response,
                    "/confidence",
                    "/data/confidence",
                    "/result/confidence");
            return new Transcription(
                    content,
                    confidence == null ? new BigDecimal("0.9000") : clamp(confidence),
                    language);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("调用 ASR 服务失败", exception);
            throw new BusinessException(502, "语音识别服务调用失败");
        }
    }

    @Override
    public Optional<IntentAnalysis> analyzeIntent(String content) {
        if (!llmConfigured()) {
            return Optional.empty();
        }
        String prompt = """
                你是古筝体验系统的意图识别器。只输出一个 JSON 对象，不要 Markdown。
                intentType 只能是 SONG_SEARCH、SONG_RECOMMEND、KNOWLEDGE_QA、UNKNOWN。
                明确要求播放某首曲子为 SONG_SEARCH；仅描述风格、情绪、场景为 SONG_RECOMMEND；
                询问古筝历史、结构、部件、技法、曲目背景为 KNOWLEDGE_QA。
                JSON 字段：intentType、confidence(0到1)、songTitle、artistName。
                用户输入：
                """ + content;
        try {
            JsonNode parsed = parseModelJson(chat(prompt, 0.0));
            return Optional.of(new IntentAnalysis(
                    parsed.path("intentType").asText("UNKNOWN"),
                    clamp(parsed.path("confidence").decimalValue()),
                    nullableText(parsed.get("songTitle")),
                    nullableText(parsed.get("artistName"))));
        } catch (Exception exception) {
            log.warn("大模型意图识别不可用，改用本地规则：{}", exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> answer(String question, List<VoiceRecords.KnowledgeRow> context) {
        if (!llmConfigured() || context.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder prompt = new StringBuilder("""
                你是古筝知识讲解员。只能依据下列资料回答问题，不得编造；资料不足时明确说明。
                回答应准确、自然、简洁，不要输出 JSON，也不要虚构引用编号。

                资料：
                """);
        for (int index = 0; index < context.size(); index++) {
            VoiceRecords.KnowledgeRow item = context.get(index);
            prompt.append(index + 1)
                    .append(". ")
                    .append(item.getTitle())
                    .append("：")
                    .append(item.getContent())
                    .append('\n');
        }
        prompt.append("\n用户问题：").append(question);
        try {
            return Optional.ofNullable(chat(prompt.toString(), 0.2))
                    .filter(StringUtils::hasText)
                    .map(String::trim);
        } catch (Exception exception) {
            log.warn("大模型问答不可用，改用知识摘要：{}", exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String modelName() {
        return StringUtils.hasText(properties.llmModel())
                ? properties.llmModel()
                : "LOCAL_GROUNDED_FALLBACK";
    }

    private String chat(String prompt, double temperature) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.llmModel());
        request.put("temperature", temperature);
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        JsonNode response = restClient.post()
                .uri(properties.llmUrl())
                .headers(headers -> authorize(headers, properties.llmApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        return firstText(response, "/choices/0/message/content", "/data/content", "/result/content");
    }

    private JsonNode parseModelJson(String value) throws Exception {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("模型返回为空");
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("```")) {
            int firstLine = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            cleaned = cleaned.substring(firstLine + 1, lastFence).trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("模型未返回 JSON");
        }
        return objectMapper.readTree(cleaned.substring(start, end + 1));
    }

    private boolean llmConfigured() {
        return StringUtils.hasText(properties.llmUrl())
                && StringUtils.hasText(properties.llmModel());
    }

    private void authorize(HttpHeaders headers, String apiKey) {
        if (StringUtils.hasText(apiKey)) {
            headers.setBearerAuth(apiKey);
        }
    }

    private String normalizeLanguage(String language) {
        int separator = language.indexOf('-');
        return separator > 0 ? language.substring(0, separator) : language;
    }

    private String firstText(JsonNode root, String... paths) {
        if (root == null) {
            return null;
        }
        for (String path : paths) {
            JsonNode node = root.at(path);
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText())) {
                return node.asText().trim();
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(JsonNode root, String... paths) {
        if (root == null) {
            return null;
        }
        for (String path : paths) {
            JsonNode node = root.at(path);
            if (node.isNumber()) {
                return node.decimalValue();
            }
        }
        return null;
    }

    private String nullableText(JsonNode node) {
        return node == null || node.isNull() || !StringUtils.hasText(node.asText())
                ? null
                : node.asText().trim();
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : value;
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String fileName;

        private NamedByteArrayResource(byte[] bytes, String fileName) {
            super(bytes);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
