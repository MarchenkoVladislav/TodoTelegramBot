package com.tbot.telegrambot.bot;

import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import com.tbot.telegrambot.db.repository.TodoRepository;
import com.tbot.telegrambot.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TodoBotServiceCmd {
    // dependencies
    private final Environment environment;
    private final TodoRepository repository;
    private final SimpleDateFormat formatter;

    // map, which is used to create tasks
    private Map<Long, Pair<Integer, TodoEntity>> createTaskMap = new ConcurrentHashMap<>();

    public Pair<String, BotKeyboardType> start() {
        return new Pair<>(environment.getProperty("hello-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
    }

    public Pair<String, BotKeyboardType> help() {
        return new Pair<>(environment.getProperty("help-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
    }

    public Pair<String, BotKeyboardType> findNotCompletedTasks(long userId) {
        Set<TodoEntity> todoEntities = repository.findAllByUserIdAndStatusOrderByIdAsc(
                userId, TodoStatus.NOT_COMPLETED
        );

        if (todoEntities.isEmpty()) {
            return new Pair<>(environment.getProperty("no-tasks-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        }

        StringBuilder stringBuilder = new StringBuilder("Not completed tasks:\n\n");
        for (TodoEntity todoEntity : todoEntities) {
            stringBuilder.append(todoEntity.toString());
        }
        return new Pair<>(stringBuilder.toString(), BotKeyboardType.DEFAULT_KEYBOARD);
    }

    public Pair<String, BotKeyboardType> allTasks(long userId) {
        Set<TodoEntity> usersTasks = repository.findAllByUserIdOrderByIdAsc(userId);
        if (!usersTasks.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("Your tasks:\n\n");
            for (TodoEntity todoEntity : usersTasks) {
                stringBuilder.append(todoEntity.toString());
            }
            return new Pair<>(stringBuilder.toString(), BotKeyboardType.DEFAULT_KEYBOARD);
        }
        return new Pair<>(environment.getProperty("no-tasks-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
    }

    public Pair<String, BotKeyboardType> findTasksByDueDate(long userId, String dateText) {
        try {
            Date dueDate = formatter.parse(dateText);
            Set<TodoEntity> todoEntities = repository.findAllByUserIdAndDueDateOrderByIdAsc(userId, dueDate);

            if (todoEntities.isEmpty()) {
                return new Pair<>(environment.getProperty("no-tasks-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
            } else {
                StringBuilder stringBuilder = new StringBuilder("Tasks on this date:\n\n");
                for (TodoEntity todoEntity : todoEntities) {
                    stringBuilder.append(todoEntity.toString());
                }
                return new Pair<>(stringBuilder.toString(), BotKeyboardType.DEFAULT_KEYBOARD);
            }
        } catch (ParseException e) {
            return new Pair<>(environment.getProperty("error-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        }
    }

    public Pair<String, BotKeyboardType> updateTaskStatus(long userId, String idText) {
        try {
            int id = Integer.parseInt(idText);
            if (!repository.existsById(id) || !repository.findById(id).getUserId().equals(userId)) {
                return new Pair<>(environment.getProperty("update-failure-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
            } else {
                TodoEntity todoEntity = repository.findById(id);
                todoEntity.setStatus(TodoStatus.COMPLETED);
                repository.save(todoEntity);
                return new Pair<>(environment.getProperty("update-success-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
            }
        } catch (NumberFormatException e) {
            return new Pair<>(environment.getProperty("error-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        }
    }

    public boolean isCreatingTask(long userId) {
        return createTaskMap.containsKey(userId);
    }

    public Pair<String, BotKeyboardType> createTask(long userId, String text) {
        switch (createTaskMap.get(userId).getKey()) {
            case 0:
                return setTaskDueDate(userId, text);
            case 1:
                return setTaskDescription(userId, text);
            case 2:
                return setTaskPriority(userId, text);
            default:
                return new Pair<>(environment.getProperty("error-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        }
    }

    public Map<Long, String> notifyUsers() {
        Map<Long, String> replies = new HashMap<>();
        for (Long user : getBotUsers()) {
            Set<TodoEntity> tasksForToday = findTasksForToday(
                    repository.findAllByUserIdAndStatusOrderByIdAsc(user, TodoStatus.NOT_COMPLETED)
            );

            StringBuilder reply = new StringBuilder();
            if (tasksForToday.isEmpty()) {
                reply.append(environment.getProperty("todo-notify-message-no-tasks"));
            } else if (tasksForToday.size() == 1) {
                reply.append(environment.getProperty("todo-notify-message-one-task"));
            } else {
                reply.append(String.format(
                        environment.getProperty("todo-notify-message-some-tasks"), tasksForToday.size()
                ));
            }

            for (TodoEntity todoEntity : tasksForToday) {
                reply.append(todoEntity.toString());
            }
            replies.put(user, reply.toString());
        }
        return replies;
    }

    private Pair<String, BotKeyboardType> setTaskDueDate(long userId, String dueDateText) {
        try {
            TodoEntity todoEntity = new TodoEntity();
            Date dueDate = formatter.parse(dueDateText);

            todoEntity.setDueDate(dueDate);
            createTaskMap.put(userId, new Pair<>(1, todoEntity));

            return new Pair<>(environment.getProperty("input-task-description"), BotKeyboardType.NO_KEYBOARD);
        } catch (ParseException e) {
            return new Pair<>(environment.getProperty("error-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private Pair<String, BotKeyboardType> setTaskDescription(long userId, String descText) {
        TodoEntity todoEntity = createTaskMap.get(userId).getValue();
        todoEntity.setDescription(descText);

        createTaskMap.get(userId).setKey(2);
        createTaskMap.get(userId).setValue(todoEntity);

        return new Pair<>(environment.getProperty("input-task-priority"), BotKeyboardType.PRIORITY_KEYBOARD);
    }

    private Pair<String, BotKeyboardType> setTaskPriority(long userId, String priorityText) {
        TodoEntity todoEntity = createTaskMap.get(userId).getValue();
        try {
            TodoPriority priority = TodoPriority.valueOf(priorityText.toUpperCase());

            todoEntity.setPriority(priority);
            todoEntity.setStatus(TodoStatus.NOT_COMPLETED);
            todoEntity.setUserId(userId);

            repository.save(todoEntity);
            createTaskMap.remove(userId);

            return new Pair<>(environment.getProperty("create-success-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        } catch (IllegalArgumentException e) {
            return new Pair<>(environment.getProperty("error-msg"), BotKeyboardType.DEFAULT_KEYBOARD);
        }
    }

    private Set<TodoEntity> findTasksForToday(Set<TodoEntity> entities) {
        return entities.stream()
                .filter(todoEntity -> {
                    LocalDate dueDate = todoEntity.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate now = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return now.getYear() == dueDate.getYear()
                            && now.getMonth() == dueDate.getMonth()
                            && now.getDayOfMonth() == dueDate.getDayOfMonth();
                }).collect(Collectors.toSet());
    }

    private Set<Long> getBotUsers() {
        return repository.findAll()
                .stream()
                .map(TodoEntity::getUserId)
                .distinct()
                .collect(Collectors.toSet());
    }
}
