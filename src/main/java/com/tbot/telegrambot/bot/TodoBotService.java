package com.tbot.telegrambot.bot;

import com.tbot.telegrambot.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoBotService extends TelegramLongPollingBot {
    // dependencies
    private final Environment environment;
    private final TodoBotServiceCmd todoBotServiceCmd;

    // these sets store the id of the user if he is performing one of these operations
    private Set<Long> taskDueDateUsers = Collections.synchronizedSet(new HashSet<>());
    private Set<Long> taskStatusUsers = Collections.synchronizedSet(new HashSet<>());

    @Override
    public String getBotToken() {
        return environment.getProperty("telegram.token");
    }

    @Override
    public String getBotUsername() {
        return environment.getProperty("telegram.username");
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        String command = message.getText()
                .replace("/", "")
                .toLowerCase();

        if (command.equals(environment.getProperty("start-cmd"))) {
            clearUserCommands(message.getChatId());
            sendMessage(message, todoBotServiceCmd.start());
        } else if (command.equals(environment.getProperty("help-cmd"))) {
            clearUserCommands(message.getChatId());
            sendMessage(message, todoBotServiceCmd.help());
        } else if (command.equals(environment.getProperty("all-tasks-cmd"))) {
            clearUserCommands(message.getChatId());
            sendMessage(message, todoBotServiceCmd.allTasks(message.getChatId()));
        } else if (command.equals(environment.getProperty("find-not-completed-cmd"))) {
            clearUserCommands(message.getChatId());
            sendMessage(message, todoBotServiceCmd.findNotCompletedTasks(message.getChatId()));
        } else if (command.equals(environment.getProperty("find-tasks-by-due-date-cmd"))) {
            clearUserCommands(message.getChatId());
            taskDueDateUsers.add(message.getChatId());
            sendMessage(message, environment.getProperty("input-date-msg"), BotKeyboardType.NO_KEYBOARD);
        } else if (command.equals(environment.getProperty("mark-task-as-completed-cmd"))) {
            taskStatusUsers.add(message.getChatId());
            sendMessage(message, environment.getProperty("input-task-id-msg"), BotKeyboardType.NO_KEYBOARD);
        } else if (command.equals(environment.getProperty("create-task-cmd"))) {
            clearUserCommands(message.getChatId());
            sendMessage(message, environment.getProperty("input-task-due-date"), BotKeyboardType.NO_KEYBOARD);
        } else if (taskDueDateUsers.contains(message.getChatId())) {
            taskDueDateUsers.remove(message.getChatId());
            sendMessage(message, todoBotServiceCmd.findTasksByDueDate(message.getChatId(), message.getText()));
        } else if (taskStatusUsers.contains(message.getChatId())) {
            taskStatusUsers.remove(message.getChatId());
            sendMessage(message, todoBotServiceCmd.updateTaskStatus(message.getChatId(), message.getText()));
        } else if (todoBotServiceCmd.isCreatingTask(message.getChatId())) {
            sendMessage(message, todoBotServiceCmd.createTask(message.getChatId(), message.getText()));
        }
    }

    @Scheduled(cron = "0 0 5 * * ?") // 5 am of the server time zone = 8 am here
    public void sendNotifyMessage() {
        todoBotServiceCmd.notifyUsers().forEach((id, text) -> {
            try {
                SendMessage sendMessage = new SendMessage(id, text);
                addDefaultKeyboard(sendMessage);
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Exception has occurred in sendMessage()", e);
            }
        });
    }

    private void clearUserCommands(long userId) {
        taskDueDateUsers.remove(userId);
        taskStatusUsers.remove(userId);
    }

    private void sendMessage(Message message, Pair<String, BotKeyboardType> content) {
        sendMessage(message, content.getKey(), content.getValue());
    }

    private void sendMessage(Message message, String text, BotKeyboardType keyboardType) {
        try {
            SendMessage sendMessage = new SendMessage(message.getChatId(), text);
            switch (keyboardType) {
                default:
                case NO_KEYBOARD:
                    break;
                case DEFAULT_KEYBOARD:
                    addDefaultKeyboard(sendMessage);
                    break;
                case PRIORITY_KEYBOARD:
                    addPriorityKeyboard(sendMessage);
                    break;
            }
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception has occurred in sendMessage()", e);
        }
    }

    private ReplyKeyboardMarkup configureReplyKeyboardMarkup(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return replyKeyboardMarkup;
    }

    private void addPriorityKeyboard(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = configureReplyKeyboardMarkup(sendMessage);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(environment.getProperty("priority-high-btn"));
        keyboardFirstRow.add(environment.getProperty("priority-middle-btn"));
        keyboardFirstRow.add(environment.getProperty("priority-low-btn"));
        keyboard.add(keyboardFirstRow);

        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    private void addDefaultKeyboard(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = configureReplyKeyboardMarkup(sendMessage);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(environment.getProperty("help-btn-text"));
        keyboardFirstRow.add(environment.getProperty("all-tasks-btn-text"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(environment.getProperty("create-task-btn-text"));
        keyboardSecondRow.add(environment.getProperty("mark-completed-btn-text"));

        KeyboardRow keyboardThirdRow = new KeyboardRow();
        keyboardThirdRow.add(environment.getProperty("find-tasks-by-due-date-btn-text"));
        keyboardThirdRow.add(environment.getProperty("find-not-completed-tasks-btn-text"));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        keyboard.add(keyboardThirdRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }
}
