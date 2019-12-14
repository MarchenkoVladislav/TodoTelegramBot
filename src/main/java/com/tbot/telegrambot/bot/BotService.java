package com.tbot.telegrambot.bot;

import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import com.tbot.telegrambot.db.repository.TodoRepository;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotService extends TelegramLongPollingBot {
    // type of the keyboard
    private enum KeyboardType {
        NO_KEYBOARD,
        DEFAULT_KEYBOARD,
        PRIORITY_KEYBOARD
    }

    private final TodoRepository repository;
    private final Environment environment;
    private final SimpleDateFormat formatter;

    // set with the ids of the users, that are using our application.
    private Map<Long, String> currentUsers = new HashMap<>();

    // these sets store the id of the user if he is performing one of these operations
    private Set<Long> taskDueDateUsers = Collections.synchronizedSet(new HashSet<>());
    private Set<Long> taskStatusUsers = Collections.synchronizedSet(new HashSet<>());

    // map, which is used to create tasks
    private Map<Long, Pair<Integer, TodoEntity>> createTaskMap = new ConcurrentHashMap<>();

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

        if (!currentUsers.containsKey(message.getChatId())) {
            currentUsers.put(message.getChatId(), message.getFrom().getUserName());
        }

        if (command.equals(environment.getProperty("start-cmd"))) {
            clearUserCommands(message.getChatId());
            startCmd(message);
        } else if (command.equals(environment.getProperty("help-cmd"))) {
            clearUserCommands(message.getChatId());
            helpCmd(message);
        } else if (command.equals(environment.getProperty("all-tasks-cmd"))) {
            clearUserCommands(message.getChatId());
            allTasksCmd(message);
        } else if (command.equals(environment.getProperty("find-not-completed-cmd"))) {
            clearUserCommands(message.getChatId());
            findNotCompletedTasksCmd(message);
        } else if (command.equals(environment.getProperty("find-tasks-by-due-date-cmd"))) {
            clearUserCommands(message.getChatId());

            if (repository.findAllByUserIdOrderByIdAsc(message.getChatId()).isEmpty()) {
                sendMessage(message, environment.getProperty("no-tasks-msg"), KeyboardType.NO_KEYBOARD);
            } else {
                taskDueDateUsers.add(message.getChatId());
                sendMessage(message, environment.getProperty("input-date-msg"), KeyboardType.NO_KEYBOARD);
            }
        } else if (command.equals(environment.getProperty("mark-task-as-completed-cmd"))) {
            clearUserCommands(message.getChatId());

            if (repository.findAllByUserIdAndStatusOrderByIdAsc(message.getChatId(), TodoStatus.NOT_COMPLETED).isEmpty()) {
                sendMessage(message, environment.getProperty("no-tasks-msg"), KeyboardType.NO_KEYBOARD);
            } else {
                taskStatusUsers.add(message.getChatId());
                sendMessage(message, environment.getProperty("input-task-id-msg"), KeyboardType.NO_KEYBOARD);
            }
        } else if (command.equals(environment.getProperty("create-task-cmd"))) {
            clearUserCommands(message.getChatId());
            createTaskMap.put(message.getChatId(), new Pair<>(0, new TodoEntity()));
            sendMessage(message, environment.getProperty("input-task-due-date"), KeyboardType.NO_KEYBOARD);
        } else if (taskDueDateUsers.contains(message.getChatId())) {
            taskDueDateUsers.remove(message.getChatId());
            findTasksByDueDate(message);
        } else if (taskStatusUsers.contains(message.getChatId())) {
            taskStatusUsers.remove(message.getChatId());
            updateTaskStatus(message);
        } else if (createTaskMap.containsKey(message.getChatId())) {
            switch (createTaskMap.get(message.getChatId()).getKey()) {
                case 0:
                    setTaskDueDate(message);
                    break;
                case 1:
                    setTaskDescription(message);
                    break;
                case 2:
                    setTaskPriority(message);
                    break;
                default:
                    sendMessage(message, environment.getProperty("error-msg"), KeyboardType.DEFAULT_KEYBOARD);
                    break;
            }
        }
    }

    @Scheduled(cron = "0 18 0 * * ?")
    public void notifyUsers() {
        for (Map.Entry<Long, String> user : currentUsers.entrySet()) {
            Set<TodoEntity> tasksForToday = findTasksForToday(
                    repository.findAllByUserIdAndStatusOrderByIdAsc(user.getKey(), TodoStatus.NOT_COMPLETED)
            );

            StringBuilder reply = new StringBuilder();
            if (tasksForToday.isEmpty()) {
                reply.append(String.format(
                        environment.getProperty("todo-notify-message-no-tasks"), user.getValue()
                ));
            } else if (tasksForToday.size() == 1) {
                reply.append(String.format(
                        environment.getProperty("todo-notify-message-one-task"), user.getValue()));
            } else {
                reply.append(String.format(
                        environment.getProperty("todo-notify-message-some-tasks"), user.getValue(), tasksForToday.size()
                ));
            }

            for (TodoEntity todoEntity : tasksForToday) {
                reply.append(todoEntity.toString());
            }
            sendNotifyMessage(user.getKey(), reply.toString());
        }
    }

    private void clearUserCommands(long userId) {
        taskDueDateUsers.remove(userId);
        taskStatusUsers.remove(userId);
    }

    private void startCmd(Message message) {
        sendMessage(message, environment.getProperty("hello-msg"), KeyboardType.DEFAULT_KEYBOARD);
    }

    private void helpCmd(Message message) {
        sendMessage(message, environment.getProperty("help-msg"), KeyboardType.DEFAULT_KEYBOARD);
    }

    private void findNotCompletedTasksCmd(Message message) {
        Set<TodoEntity> todoEntities = findAllNotCompleted(message.getChatId());
        if (todoEntities.isEmpty()) {
            sendMessage(message, environment.getProperty("no-tasks-msg"), KeyboardType.DEFAULT_KEYBOARD);
        } else {
            StringBuilder stringBuilder = new StringBuilder("Not completed tasks:\n\n");
            for (TodoEntity todoEntity : todoEntities) {
                stringBuilder.append(todoEntity.toString());
            }
            sendMessage(message, stringBuilder.toString(), KeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private void allTasksCmd(Message message) {
        Set<TodoEntity> usersTasks = getAllToDosByUser(message.getChatId());
        if (!usersTasks.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("Your tasks:\n\n");
            for (TodoEntity todoEntity : usersTasks) {
                stringBuilder.append(todoEntity.toString());
            }
            sendMessage(message, stringBuilder.toString(), KeyboardType.DEFAULT_KEYBOARD);
        } else {
            sendMessage(message, environment.getProperty("no-tasks-msg"), KeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private void findTasksByDueDate(Message message) {
        try {
            Date due = formatter.parse(message.getText());

            Set<TodoEntity> todoEntities = findAllByDueDate(message.getChatId(), due);
            if (todoEntities.isEmpty()) {
                sendMessage(message, environment.getProperty("no-tasks-msg"), KeyboardType.DEFAULT_KEYBOARD);
            } else {
                StringBuilder stringBuilder = new StringBuilder("Tasks on this date:\n\n");
                for (TodoEntity todoEntity : todoEntities) {
                    stringBuilder.append(todoEntity.toString());
                }
                sendMessage(message, stringBuilder.toString(), KeyboardType.DEFAULT_KEYBOARD);
            }
        } catch (ParseException e) {
            sendMessage(message, environment.getProperty("error-msg"), KeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private void updateTaskStatus(Message message) {
        try {
            int id = Integer.parseInt(message.getText());
            if (!repository.existsById(id) || !repository.findById(id).getUserId().equals(message.getChatId())) {
                sendMessage(message, environment.getProperty("error-msg"), KeyboardType.DEFAULT_KEYBOARD);
            } else {
                markCompleted(id);
                sendMessage(message, environment.getProperty("update-success-msg"), KeyboardType.DEFAULT_KEYBOARD);
            }
        } catch (NumberFormatException e) {
            sendMessage(message, environment.getProperty("error-msg"), KeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private void setTaskDueDate(Message message) {
        try {
            TodoEntity todoEntity = createTaskMap.get(message.getChatId()).getValue();
            Date dueDate = formatter.parse(message.getText());

            todoEntity.setDueDate(dueDate);
            sendMessage(message, environment.getProperty("input-task-description"), KeyboardType.NO_KEYBOARD);

            createTaskMap.get(message.getChatId()).setKey(1);
            createTaskMap.get(message.getChatId()).setValue(todoEntity);
        } catch (ParseException e) {
            sendMessage(message, environment.getProperty("error-msg"), KeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private void setTaskDescription(Message message) {
        TodoEntity todoEntity = createTaskMap.get(message.getChatId()).getValue();
        todoEntity.setDescription(message.getText());
        sendMessage(message, environment.getProperty("input-task-priority"), KeyboardType.PRIORITY_KEYBOARD);

        createTaskMap.get(message.getChatId()).setKey(2);
        createTaskMap.get(message.getChatId()).setValue(todoEntity);
    }

    private void setTaskPriority(Message message) {
        TodoEntity todoEntity = createTaskMap.get(message.getChatId()).getValue();
        try {
            TodoPriority priority = TodoPriority.valueOf(message.getText().toUpperCase());

            todoEntity.setPriority(priority);
            todoEntity.setStatus(TodoStatus.NOT_COMPLETED);
            todoEntity.setUserId(message.getChatId());
            sendMessage(message, environment.getProperty("create-success-msg"), KeyboardType.DEFAULT_KEYBOARD);

            createTask(todoEntity);
            createTaskMap.remove(message.getChatId());
        } catch (IllegalArgumentException e) {
            sendMessage(message, environment.getProperty("error-msg"), KeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private void sendNotifyMessage(long chatId, String text) {
        try {
            SendMessage sendMessage = new SendMessage(chatId, text);
            addDefaultKeyboard(sendMessage);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception has occurred in sendMessage()", e);
        }
    }

    private void sendMessage(Message message, String text, KeyboardType keyboardType) {
        try {
            SendMessage sendMessage = new SendMessage(message.getChatId(), text);
            switch (keyboardType) {
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
        replyKeyboardMarkup.setOneTimeKeyboard(true);
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

    private void createTask(TodoEntity todoEntity) {
        repository.save(todoEntity);
    }

    private Set<TodoEntity> getAllToDosByUser(Long user) {
        return repository.findAllByUserIdOrderByIdAsc(user);
    }

    private void markCompleted(int task) {
        TodoEntity todoEntity = repository.findById(task);
        if (todoEntity != null) {
            todoEntity.setStatus(TodoStatus.COMPLETED);
            repository.save(todoEntity);
        }
    }

    private Set<TodoEntity> findAllNotCompleted(Long user) {
        return repository.findAllByUserIdAndStatusOrderByIdAsc(user, TodoStatus.NOT_COMPLETED);
    }

    private Set<TodoEntity> findAllByDueDate(Long user, Date date) {
        return repository.findAllByUserIdAndDueDateOrderByIdAsc(user, date);
    }

    private static Set<TodoEntity> findTasksForToday(Set<TodoEntity> entities) {
        return entities.stream()
                .filter(todoEntity -> {
                    LocalDate dueDate = todoEntity.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate now = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return now.getYear() == dueDate.getYear()
                            && now.getMonth() == dueDate.getMonth()
                            && now.getDayOfMonth() == dueDate.getDayOfMonth();
                }).collect(Collectors.toSet());
    }
}
