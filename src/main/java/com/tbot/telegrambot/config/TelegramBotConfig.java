package com.tbot.telegrambot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:telegram.properties")
@Data
public class TelegramBotConfig {
    @Value("${telegram.username}")
    private String username;

    @Value("${telegram.token}")
    private String token;
}
