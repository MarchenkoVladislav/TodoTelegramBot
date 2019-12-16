package com.tbot.telegrambot;

import com.tbot.telegrambot.bot.TodoBotService;
import com.tbot.telegrambot.db.repository.TodoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BotServiceTest {
    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoBotService bot;
}
