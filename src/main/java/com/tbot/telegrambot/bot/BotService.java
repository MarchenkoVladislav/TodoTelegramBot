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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BotService extends TelegramLongPollingBot {
    private final TodoRepository repository;
    private final TelegramBotConfig config;
    private final static String helpBtn = "Help";
    private final static String allTasksBtn = "All tasks";
    private final static String helloMsg = "Hello!\n" +
            "I'm ToDoBot.\n" +
            "You can add your tasks, show the current tasks and manage it.\n" +
            "For more information, please, click help button.";
    private final static String helpMsg = "You can:";
    private final static String helpMsg1 ="Create task - for this you should to send a string:\n" +
            "cmd$create*text${text of task}*priority${HIGH/LOW/MIDDLE}\n" +
            "*due${date in format dd-mm-yyyy}\n";
    private final static String helpMsg2 = "Show all tasks - for this you should to click 'All tasks' button";
    private final static String helpMsg3 = "Mark task completed - for this you should to send a string:\n" +
            "cmd$mc*id${task id}";
    private final static String helpMsg4 = "Find tasks by status - for this you should to send a string:\n" +
            "cmd$fbs*status${COMPLETED/NOT_COMPLETED}";
    private final static String helpMsg5 = "Find tasks by due date - for this you should to send a string:\n" +
            "cmd$fbd*due${date in format dd-mm-yyyy}";
    private final static String errorMSG ="Invalid command! Click 'Help' to get more information.";
    private final static String createTaskMsg = "Task was created successfully!";
    private final static String updateStatusMsg = "Task status was updated successfully!";

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        String txt = msg.getText();
        if (txt.equals("/start")) {
            sendMsg(msg, helloMsg);
        }
        else if(txt.equals("Help")) {
            sendMsg(msg, helpMsg);
            sendMsg(msg, helpMsg1);
            sendMsg(msg, helpMsg2);
            sendMsg(msg, helpMsg3);
            sendMsg(msg, helpMsg4);
            sendMsg(msg, helpMsg5);
        }
        else if(txt.equals("All tasks")) {
            Set<TodoEntity> usersTasks = getAllToDosByUser(msg.getChatId());
            if (!usersTasks.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder("Your tasks\n\n");
                for (TodoEntity todoEntity : usersTasks) {
                    stringBuilder.append(todoEntity.toString());
                }
                sendMsg(msg, stringBuilder.toString());
            }
            else {
                sendMsg(msg, "You do not have some tasks!");
            }

        }
        else {
            Map<String, String> params = parseStringIntoParams(txt);
            if (!params.containsKey("cmd")) {
                sendMsg(msg, errorMSG);
            }
            else if(params.get("cmd").equals("create")) {
                createTask(params, msg.getChatId());
                sendMsg(msg, createTaskMsg);
            }
            else if(params.get("cmd").equals("mc")) {
                markCompleted(Integer.parseInt(params.get("id")));
                sendMsg(msg, updateStatusMsg);
            }
            else if(params.get("cmd").equals("fbs")) {
                Set<TodoEntity> statusTasks = findAllByStatus(msg.getChatId(), TodoStatus.valueOf(params.get("status")));
                if (!statusTasks.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder("Tasks:\n\n");
                    for (TodoEntity todoEntity : statusTasks) {
                        stringBuilder.append(todoEntity.toString());
                    }
                    sendMsg(msg, stringBuilder.toString());
                }
                else {
                    sendMsg(msg, "You do not have tasks with this status!");
                }
            }
            else if(params.get("cmd").equals("fbd")) {
                Set<TodoEntity> dateTasks = findAllforDate(msg.getChatId(), params.get("due"));
                if(!dateTasks.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder("Tasks:\n\n");
                    for (TodoEntity todoEntity : dateTasks) {
                        stringBuilder.append(todoEntity.toString());
                    }
                    sendMsg(msg, stringBuilder.toString());
                }
                else {
                    sendMsg(msg, "You do not have tasks on this date!");
                }
            }
            else {
                sendMsg(msg, errorMSG);
            }
        }
    }

    private void sendMsg(Message msg, String text) {
        SendMessage s = new SendMessage();

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        s.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(helpBtn);
        keyboardFirstRow.add(allTasksBtn);

        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        s.setChatId(msg.getChatId());
        s.setText(text);
        try {
            execute(s);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }


    private TodoEntity createTask(Map<String, String> taskParams, long user) {
        try {
           TodoEntity todoEntity = new TodoEntity();
           SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
           todoEntity.setDue(formatter.parse(taskParams.get("due")));
           todoEntity.setPriority(TodoPriority.valueOf(taskParams.get("priority")));
           todoEntity.setStatus(TodoStatus.NOT_COMPLETED);
           todoEntity.setText(taskParams.get("text"));
           todoEntity.setUserID(user);
           return repository.save(todoEntity);
        } catch (ParseException e) {
           InvalidDateException ibfex = new InvalidDateException("create", taskParams.get("id"));
           ibfex.initCause(e);
           throw ibfex;
        }
    }

    private Set<TodoEntity> getAllToDosByUser(Long user) {
        return repository.findAllByUserIDOrderByIdAsc(user);
    }

    private TodoEntity markCompleted(int task) {
        TodoEntity todoEntity = repository.findById(task);
        todoEntity.setStatus(TodoStatus.COMPLETED);
        return repository.save(todoEntity);
    }

    private Set<TodoEntity> findAllByStatus(Long user, TodoStatus status) {
        return repository.findAllByUserIDAndStatusOrderByIdAsc(user, status);
    }

    private Set<TodoEntity> findAllforDate(Long user, String date) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            Date due = formatter.parse(date);
            return repository.findAllByUserIDAndDueOrderByIdAsc(user, due);
        } catch (ParseException e) {
            InvalidDateException ibfex = new InvalidDateException("create", "XXX");
            ibfex.initCause(e);
            throw ibfex;
        }
    }

    private Map<String, String> parseStringIntoParams(String string) {
        String [] tmp = string.split("\\*");
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
