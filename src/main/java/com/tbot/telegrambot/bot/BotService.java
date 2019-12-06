package com.tbot.telegrambot.bot;

import com.tbot.telegrambot.config.TelegramBotConfig;
import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import com.tbot.telegrambot.db.repository.TodoRepository;
import com.tbot.telegrambot.exception.InvalidDateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Vladislav Marchenko
 */
@Service
@RequiredArgsConstructor
public class BotService extends TelegramLongPollingBot {
    private final TodoRepository repository;
    private final TelegramBotConfig config;

    // key1$value1@key2$value2@

    @Override
    public void onUpdateReceived(Update update) {
        // create task

        // mark completed

        // find all by status(completed and not completed)

        // find all for date
    }

    private TodoEntity createTask(String taskString) {
        Map<String, String> taskParams = parseStringIntoParams(taskString);
        try {
           TodoEntity todoEntity = new TodoEntity();
           SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
           todoEntity.setDue(formatter.parse(taskParams.get("due")));
           todoEntity.setPriority(TodoPriority.valueOf(taskParams.get("priority")));
           todoEntity.setStatus(TodoStatus.NOT_COMPLTED);
           todoEntity.setText(taskParams.get("text"));
           todoEntity.setUser(Integer.parseInt(taskParams.get("user")));
           return repository.save(todoEntity);
        } catch (ParseException e) {
           InvalidDateException ibfex = new InvalidDateException("create", taskParams.get("id"));
           ibfex.initCause(e);
           throw ibfex;
        }
    }

    private TodoEntity markCompleted(String taskString) {
        Map<String, String> taskParams = parseStringIntoParams(taskString);
        TodoEntity todoEntity = repository.getOne(Integer.parseInt(taskParams.get("id")));
        todoEntity.setStatus(TodoStatus.COMPLETED);
        return repository.save(todoEntity);
    }

    private Set<TodoEntity> findAllByStatus(String taskString) {
        Map<String, String> taskParams = parseStringIntoParams(taskString);
        return repository.findAllByUserAndStatus(Integer.parseInt(taskParams.get("user")),
                TodoStatus.valueOf(taskParams.get("status")));
    }

    private Set<TodoEntity> findAllforDate(String taskString) {
        Map<String, String> taskParams = parseStringIntoParams(taskString);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            Date date = formatter.parse(taskParams.get("due"));
            return repository.findAllByUserAndDue(Integer.parseInt(taskParams.get("user")), date);
        } catch (ParseException e) {
            InvalidDateException ibfex = new InvalidDateException("findForDate", taskParams.get("id"));
            ibfex.initCause(e);
            throw ibfex;
        }
    }

    private Map<String, String> parseStringIntoParams(String string) {
        String [] tmp = string.split("@");
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < tmp.length; i++) {
            String [] param = tmp[i].split("\\$");
            params.put(param[0], param[1]);
        }
        return  params;
    }


    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }
}
