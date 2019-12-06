package com.tbot.telegrambot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.telegram.telegrambots.bots.DefaultBotOptions;

/**
 * @author Vladislav Marchenko
 */
@Configuration
@PropertySource("classpath:telegram.properties")
@Data
public class TelegramBotConfig {
    @Value("${telegram.username}")
    private String username;

    @Value("${telegram.token}")
    private String token;

    @Value("${telegram.path}")
    private String path;

    @Value("${telegram.threads}")
    private Integer threads;

    @Bean
    public DefaultBotOptions defaultBotOptions(){
        DefaultBotOptions defaultBotOptions = new DefaultBotOptions();
        defaultBotOptions.setMaxThreads(threads);
        return defaultBotOptions;
    }
}
