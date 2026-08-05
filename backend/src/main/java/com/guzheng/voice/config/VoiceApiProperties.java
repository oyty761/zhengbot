package com.guzheng.voice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * 从 Git 忽略的本地文件读取语音供应商配置，同时允许环境变量覆盖。
 * 环境变量优先级高于 properties 文件，适合容器和生产环境。
 */
@Slf4j
@Component
public class VoiceApiProperties {

    private final Properties properties = new Properties();

    public VoiceApiProperties() {
        Path configPath = locateConfig();
        if (configPath == null) {
            log.info("未找到 voice-api.properties，将仅使用环境变量配置语音服务");
            return;
        }
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
            log.info("已加载语音服务配置：{}", configPath.toAbsolutePath().normalize());
        } catch (IOException exception) {
            log.warn("读取语音服务配置失败：{}", configPath, exception);
        }
    }

    public String asrUrl() {
        return value("VOICE_ASR_URL", "voice.asr.url", "");
    }

    public String asrApiKey() {
        return value("VOICE_ASR_API_KEY", "voice.asr.api-key", "");
    }

    public String asrModel() {
        return value("VOICE_ASR_MODEL", "voice.asr.model", "whisper-1");
    }

    public String llmUrl() {
        return value("VOICE_LLM_URL", "voice.llm.url", "");
    }

    public String llmApiKey() {
        return value("VOICE_LLM_API_KEY", "voice.llm.api-key", "");
    }

    public String llmModel() {
        return value("VOICE_LLM_MODEL", "voice.llm.model", "");
    }

    public String audioStorageDir() {
        return value("VOICE_AUDIO_STORAGE_DIR", "voice.audio.storage-dir", "./data/voice");
    }

    private String value(String environmentName, String propertyName, String defaultValue) {
        String environmentValue = System.getenv(environmentName);
        if (StringUtils.hasText(environmentValue)) {
            return environmentValue.trim();
        }
        String systemValue = System.getProperty(propertyName);
        if (StringUtils.hasText(systemValue)) {
            return systemValue.trim();
        }
        String fileValue = properties.getProperty(propertyName);
        return StringUtils.hasText(fileValue) ? fileValue.trim() : defaultValue;
    }

    private Path locateConfig() {
        String explicitPath = System.getenv("VOICE_CONFIG_FILE");
        if (StringUtils.hasText(explicitPath)) {
            Path path = Path.of(explicitPath.trim());
            return Files.isRegularFile(path) ? path : null;
        }
        return List.of(
                        Path.of("config", "voice-api.properties"),
                        Path.of("backend", "config", "voice-api.properties"))
                .stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
    }
}
