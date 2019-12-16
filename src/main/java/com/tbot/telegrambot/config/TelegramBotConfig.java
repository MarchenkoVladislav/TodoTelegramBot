package com.tbot.telegrambot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Configuration
@EnableScheduling
@PropertySources({
        @PropertySource("classpath:telegram.properties"),
        @PropertySource("classpath:messages.properties"),
        @PropertySource("classpath:commands.properties")
})
public class TelegramBotConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
    }
}
