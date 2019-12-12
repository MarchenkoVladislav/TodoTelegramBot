package com.tbot.telegrambot.bot;

import com.tbot.telegrambot.config.TelegramBotConfig;
import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import com.tbot.telegrambot.db.repository.TodoRepository;
import com.tbot.telegrambot.util.Pair;
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
    private final static String createTaskBtn = "Create task";
    private final static String markCompletedBtn = "Mark completed";
    private final static String findTasksByStatusBtn = "Find not completed";
    private final static String findTasksByDueBtn = "Find tasks by due";


    private final static String helloMsg = "Hello!\n" +
            "I'm ToDoBot.\n" +
            "You can add your tasks, show the current tasks and manage it.\n" +
            "For more information, please, click help button.";
    private final static String helpMsg = "You can:";
    private final static String helpMsg1 ="Create task - for this you should to click 'Create task' and follow instructions";
    private final static String helpMsg2 = "Show all tasks - for this you should to click 'All tasks' button";
    private final static String helpMsg3 = "Mark task completed - for this you should to click 'Mark completed' and follow instructions";
    private final static String helpMsg4 = "Find not completed tasks - for this you should to click 'Find not completed' and follow instructions";
    private final static String helpMsg5 = "Find tasks by due date - for this you should to click 'Find tasks by due' and follow instructions";
    private final static String errorMSG ="Invalid command! Try again";
    private final static String createTaskMsg = "Task was created successfully!";
    private final static String updateStatusMsg = "Task status was updated successfully!";

    private Map<Long,Boolean> dueMap  = new HashMap<>();
    private Map<Long,Boolean> markMap  = new HashMap<>();
    private Map<Long, Pair<Integer, TodoEntity>> createMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        String txt = msg.getText();
        if (txt.equals("/start")) {
            sendMsg(msg, helloMsg, true);
        }
        else if(txt.equals("Help")) {
            sendMsg(msg, helpMsg,true);
            sendMsg(msg, helpMsg1, true);
            sendMsg(msg, helpMsg2, true);
            sendMsg(msg, helpMsg3, true);
            sendMsg(msg, helpMsg4, true);
            sendMsg(msg, helpMsg5, true);
        }
        else if(txt.equals("All tasks")) {
            Set<TodoEntity> usersTasks = getAllToDosByUser(msg.getChatId());
            if (!usersTasks.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder("Your tasks\n\n");
                for (TodoEntity todoEntity : usersTasks) {
                    stringBuilder.append(todoEntity.toString());
                }
                sendMsg(msg, stringBuilder.toString(), true);
            }
            else {
                sendMsg(msg, "You do not have any tasks!", true);
            }

        }
        else if (txt.equals("Find not completed")){
            Set<TodoEntity>  todoEntities = findAllByStatus(msg.getChatId(), TodoStatus.NOT_COMPLETED);
            if (todoEntities.isEmpty()) {
                sendMsg(msg, "You do not have not completed tasks", true);
            }
            else{
                StringBuilder stringBuilder = new StringBuilder("Not completed tasks\n\n");
                for (TodoEntity todoEntity : todoEntities) {
                    stringBuilder.append(todoEntity.toString());
                }
                sendMsg(msg, stringBuilder.toString(), true);
            }
        }
        else if (txt.equals("Find tasks by due")) {
            dueMap.put(msg.getChatId(), true);
            sendMsg(msg, "Enter a date, please(dd-mm-yyyy)", false);
        }
        else if (txt.equals("Mark completed")) {
            markMap.put(msg.getChatId(), true);
            sendMsg(msg, "Enter a number of task, please", false);
        }
        else if (txt.equals("Create task")) {
            createMap.put(msg.getChatId(), new Pair<>(0, new TodoEntity()));
            sendMsg(msg, "Enter a date of task, please(dd-mm-yyyy)", false);
        }
        else if(dueMap.containsKey(msg.getChatId())) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
                Date due = formatter.parse(txt);
                dueMap.remove(msg.getChatId());
                Set<TodoEntity>  todoEntities = findAllforDate(msg.getChatId(), due);
                if (todoEntities.isEmpty()) {
                    sendMsg(msg, "You do not have tasks on this date", true);
                }
                else{
                    StringBuilder stringBuilder = new StringBuilder("Tasks on this date\n\n");
                    for (TodoEntity todoEntity : todoEntities) {
                        stringBuilder.append(todoEntity.toString());
                    }
                    sendMsg(msg, stringBuilder.toString(), true);
                }
            }
            catch(ParseException e) {
                sendMsg(msg, errorMSG, true);
            }


        }
        else if(markMap.containsKey(msg.getChatId())) {
            TodoEntity todoEntity = repository.findById(Integer.parseInt(txt));
            if (todoEntity == null) {
                sendMsg(msg,errorMSG, true);
            }
            else {
                markMap.remove(msg.getChatId());
                markCompleted(Integer.parseInt(txt));
                sendMsg(msg, "Task status was changed successfully", true);
            }
        }
        else if(createMap.containsKey(msg.getChatId())) {
            if (createMap.get(msg.getChatId()).getKey().equals(0)) {
                TodoEntity todoEntity = createMap.get(msg.getChatId()).getValue();
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
                    Date due = formatter.parse(txt);
                    todoEntity.setDue(due);
                    sendMsg(msg, "Enter a text, please", false);
                    createMap.get(msg.getChatId()).setKey(1);
                    createMap.get(msg.getChatId()).setValue(todoEntity);
                }
                catch(ParseException e) {
                    sendMsg(msg, errorMSG, true);
                }
            }
            else if (createMap.get(msg.getChatId()).getKey().equals(1)) {
                TodoEntity todoEntity = createMap.get(msg.getChatId()).getValue();
                todoEntity.setText(txt);
                sendMsg(msg, "Enter a priority(HIGH/MIDDLE/LOW), please", false);
                createMap.get(msg.getChatId()).setKey(2);
                createMap.get(msg.getChatId()).setValue(todoEntity);
            }
            else if (createMap.get(msg.getChatId()).getKey().equals(2)) {
                TodoEntity todoEntity = createMap.get(msg.getChatId()).getValue();
                if (!txt.equals("LOW") && !txt.equals("MIDDLE") && !txt.equals("HIGH")) {
                    sendMsg(msg,errorMSG,true);
                }
                else {
                    todoEntity.setPriority(TodoPriority.valueOf(txt));
                    todoEntity.setStatus(TodoStatus.NOT_COMPLETED);
                    todoEntity.setUserID(msg.getChatId());
                    sendMsg(msg, "Your task was successfully added", true);
                    createTask(todoEntity);
                    createMap.remove(msg.getChatId());
                }
            }
        }
    }

    private void sendMsg(Message msg, String text, boolean flag) {
        SendMessage s = new SendMessage();

        if (flag) {
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            s.setReplyMarkup(replyKeyboardMarkup);
            replyKeyboardMarkup.setSelective(true);
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(false);

            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow keyboardFirstRow = new KeyboardRow();
            keyboardFirstRow.add(helpBtn);
            keyboardFirstRow.add(allTasksBtn);

            KeyboardRow keyboardSecondRow = new KeyboardRow();
            keyboardSecondRow.add(createTaskBtn);
            keyboardSecondRow.add(markCompletedBtn);

            KeyboardRow keyboardThirdRow = new KeyboardRow();
            keyboardThirdRow.add(findTasksByDueBtn);
            keyboardThirdRow.add(findTasksByStatusBtn);

            keyboard.add(keyboardFirstRow);
            keyboard.add(keyboardSecondRow);
            keyboard.add(keyboardThirdRow);
            replyKeyboardMarkup.setKeyboard(keyboard);
        }
        s.setChatId(msg.getChatId());
        s.setText(text);
        try {
            execute(s);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private TodoEntity createTask(TodoEntity todoEntity) {
        return repository.save(todoEntity);
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

    private Set<TodoEntity> findAllforDate(Long user, Date date) {
        return repository.findAllByUserIDAndDueOrderByIdAsc(user, date);
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
