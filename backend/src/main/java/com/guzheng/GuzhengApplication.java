package com.guzheng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.guzheng.common",
        "com.guzheng.config",
        "com.guzheng.composition",
        "com.guzheng.controller",
        "com.guzheng.mapper",
        "com.guzheng.service",
        "com.guzheng.songbook"
})
public class GuzhengApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuzhengApplication.class, args);
    }
}
