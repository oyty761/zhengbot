package com.guzheng.voice;

import com.guzheng.voice.controller.VoiceInteractionController;
import com.guzheng.voice.mapper.VoiceInteractionMapper;
import com.guzheng.voice.service.VoiceInteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 验证主应用能够扫描语音模块并解析其 MyBatis 映射。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class VoiceApplicationContextTest {

    @Autowired
    private VoiceInteractionController controller;

    @Autowired
    private VoiceInteractionService service;

    @Autowired
    private VoiceInteractionMapper mapper;

    @Test
    void voiceModuleIsLoadedByMainApplication() {
        assertNotNull(controller);
        assertNotNull(service);
        assertNotNull(mapper);
    }
}
