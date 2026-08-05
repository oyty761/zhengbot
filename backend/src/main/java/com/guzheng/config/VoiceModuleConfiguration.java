package com.guzheng.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 主应用使用显式扫描白名单；此装配类在白名单内，并负责加载独立的语音模块。
 */
@Configuration
@ComponentScan("com.guzheng.voice")
public class VoiceModuleConfiguration {
}
