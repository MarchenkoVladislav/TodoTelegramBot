package com.tbot.telegrambot;

import com.tbot.telegrambot.bot.BotKeyboardType;
import com.tbot.telegrambot.bot.TodoBotServiceCmd;
import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import com.tbot.telegrambot.db.repository.TodoRepository;
import com.tbot.telegrambot.util.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.telegram.telegrambots.ApiContextInitializer;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BotServiceTest {
    private static final long USER_WITH_NO_TASKS = 0;
    private static final long USER_WITH_SOME_TASKS = 1;
    private static final int EXISTING_TASK_ID = 1;
    private static final int NON_EXISTING_TASK_ID = 1000;
    private static final List<TodoEntity> DATABASE = new ArrayList<>();
    private static final Set<TodoEntity> TASKS = Stream.of(
            new TodoEntity(
                    1,
                    USER_WITH_SOME_TASKS,
                    "desc",
                    TodoStatus.NOT_COMPLETED,
                    TodoPriority.LOW,
                    new Date()
            ),
            new TodoEntity(
                    2,
                    USER_WITH_SOME_TASKS,
                    "desc",
                    TodoStatus.NOT_COMPLETED,
                    TodoPriority.LOW,
                    new Date()
            )).collect(Collectors.toSet());

    static {
        ApiContextInitializer.init();
    }

    @MockBean
    private TodoRepository todoRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private SimpleDateFormat formatter;

    @Autowired
    private TodoBotServiceCmd botServiceCmd;

    @Before
    public void setUp() throws Exception {
        Mockito.when(todoRepository.findAllByUserIdAndStatusOrderByIdAsc(USER_WITH_NO_TASKS, TodoStatus.NOT_COMPLETED))
                .thenReturn(new HashSet<>());
        Mockito.when(todoRepository.findAllByUserIdAndStatusOrderByIdAsc(USER_WITH_SOME_TASKS, TodoStatus.NOT_COMPLETED))
                .thenReturn(TASKS);
        Mockito.when(todoRepository.findAllByUserIdOrderByIdAsc(USER_WITH_NO_TASKS))
                .thenReturn(new HashSet<>());
        Mockito.when(todoRepository.findAllByUserIdOrderByIdAsc(USER_WITH_SOME_TASKS))
                .thenReturn(TASKS);
        Mockito.when(todoRepository.findAllByUserIdAndDueDateOrderByIdAsc(USER_WITH_NO_TASKS,
                formatter.parse(formatter.format(new Date()))))
                .thenReturn(new HashSet<>());
        Mockito.when(todoRepository.findAllByUserIdAndDueDateOrderByIdAsc(USER_WITH_SOME_TASKS,
                formatter.parse(formatter.format(new Date()))))
                .thenReturn(TASKS);
        Mockito.when(todoRepository.findById(EXISTING_TASK_ID))
                .thenReturn(
                        new TodoEntity(
                                1,
                                USER_WITH_SOME_TASKS,
                                "desc",
                                TodoStatus.NOT_COMPLETED,
                                TodoPriority.LOW,
                                new Date()
                        )
                );
        Mockito.when(todoRepository.findById(NON_EXISTING_TASK_ID))
                .thenReturn(null);
        Mockito.when(todoRepository.existsById(EXISTING_TASK_ID))
                .thenReturn(true);
        Mockito.when(todoRepository.existsById(NON_EXISTING_TASK_ID))
                .thenReturn(false);
        Mockito.when(todoRepository.findAll())
                .thenReturn(new ArrayList<>(TASKS));
        Mockito.when(todoRepository.save(Mockito.any(TodoEntity.class)))
                .thenAnswer(invocation -> DATABASE.add((TodoEntity) invocation.getArguments()[0]));
    }

    @AfterEach
    public void cleanUp() {
        DATABASE.clear();
    }

    @Test
    public void test_Start() {
        Pair<String, BotKeyboardType> start = botServiceCmd.start();
        Assert.assertEquals(environment.getProperty("hello-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_Help() {
        Pair<String, BotKeyboardType> start = botServiceCmd.help();
        Assert.assertEquals(environment.getProperty("help-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_findNotCompletedTasks_Empty() {
        Pair<String, BotKeyboardType> start = botServiceCmd.findNotCompletedTasks(USER_WITH_NO_TASKS);
        Assert.assertEquals(environment.getProperty("no-tasks-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_findNotCompletedTasks_NotEmpty() {
        Pair<String, BotKeyboardType> start = botServiceCmd.findNotCompletedTasks(USER_WITH_SOME_TASKS);
        Assert.assertEquals(TASKS.stream()
                .map(TodoEntity::toString)
                .reduce("Not completed tasks:\n\n", (left, right) -> left + right), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_allTasks_Empty() {
        Pair<String, BotKeyboardType> start = botServiceCmd.allTasks(USER_WITH_NO_TASKS);
        Assert.assertEquals(environment.getProperty("no-tasks-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_allTasks_NotEmpty() {
        Pair<String, BotKeyboardType> start = botServiceCmd.allTasks(USER_WITH_SOME_TASKS);
        Assert.assertEquals(TASKS.stream()
                .map(TodoEntity::toString)
                .reduce("Your tasks:\n\n", (left, right) -> left + right), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_findTasksByDueDate_Empty() {
        Pair<String, BotKeyboardType> start = botServiceCmd.findTasksByDueDate(USER_WITH_NO_TASKS,
                formatter.format(new Date()));
        Assert.assertEquals(environment.getProperty("no-tasks-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_findTasksByDueDate_NotEmpty() {
        Pair<String, BotKeyboardType> start = botServiceCmd.findTasksByDueDate(USER_WITH_SOME_TASKS,
                formatter.format(new Date()));
        Assert.assertEquals(TASKS.stream()
                .map(TodoEntity::toString)
                .reduce("Tasks on this date:\n\n", (left, right) -> left + right), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_findTasksByDueDate_NotCorrectDate() {
        Pair<String, BotKeyboardType> start = botServiceCmd.findTasksByDueDate(USER_WITH_SOME_TASKS,
                "incorrect_date");
        Assert.assertEquals(environment.getProperty("error-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_updateTaskStatus_Existing() {
        Pair<String, BotKeyboardType> start = botServiceCmd.updateTaskStatus(
                USER_WITH_SOME_TASKS,
                "1"
        );
        Assert.assertEquals(environment.getProperty("update-success-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_updateTaskStatus_NotExisting() {
        Pair<String, BotKeyboardType> start = botServiceCmd.updateTaskStatus(
                USER_WITH_SOME_TASKS,
                "1000"
        );
        Assert.assertEquals(environment.getProperty("update-failure-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_updateTaskStatus_NotCorrectId() {
        Pair<String, BotKeyboardType> start = botServiceCmd.updateTaskStatus(
                USER_WITH_SOME_TASKS,
                "incorrect_ID"
        );
        Assert.assertEquals(environment.getProperty("error-msg"), start.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, start.getValue());
    }

    @Test
    public void test_notifyUsers() {
        Map<Long, String> result = botServiceCmd.notifyUsers();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
                TASKS.stream()
                        .map(TodoEntity::toString)
                        .reduce("Good morning! Today you have 2 tasks to complete!\nHere they are:\n",
                                (left, right) -> left + right),
                result.get(USER_WITH_SOME_TASKS)
        );
    }

    public void test_createTask() throws Exception {
        botServiceCmd.setIsCreatingTask(USER_WITH_SOME_TASKS);

        Pair<String, BotKeyboardType> incorrectDate = botServiceCmd.createTask(
                USER_WITH_SOME_TASKS,
                "incorrect_date"
        );
        Assert.assertEquals(environment.getProperty("error-msg"), incorrectDate.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, incorrectDate.getValue());

        Pair<String, BotKeyboardType> correctDate = botServiceCmd.createTask(
                USER_WITH_SOME_TASKS,
                formatter.format(new Date())
        );
        Assert.assertEquals(environment.getProperty("input-task-description"), correctDate.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, correctDate.getValue());

        Pair<String, BotKeyboardType> correctDesc = botServiceCmd.createTask(
                USER_WITH_SOME_TASKS,
                "description"
        );
        Assert.assertEquals(environment.getProperty("input-task-priority"), correctDesc.getKey());
        Assert.assertEquals(BotKeyboardType.PRIORITY_KEYBOARD, correctDesc.getValue());

        Pair<String, BotKeyboardType> incorrectPriority = botServiceCmd.createTask(
                USER_WITH_SOME_TASKS,
                "incorrect_priority"
        );
        Assert.assertEquals(environment.getProperty("error-msg"), incorrectPriority.getKey());
        Assert.assertEquals(BotKeyboardType.PRIORITY_KEYBOARD, incorrectPriority.getValue());

        Pair<String, BotKeyboardType> correctPriority = botServiceCmd.createTask(
                USER_WITH_SOME_TASKS,
                "HIGH"
        );
        Assert.assertEquals(environment.getProperty("create-success-msg"), correctPriority.getKey());
        Assert.assertEquals(BotKeyboardType.DEFAULT_KEYBOARD, correctPriority.getValue());

        Assert.assertEquals(USER_WITH_SOME_TASKS, (long) DATABASE.get(0).getUserId());
        Assert.assertEquals(formatter.parse(formatter.format(new Date())), DATABASE.get(0).getDueDate());
        Assert.assertEquals("description", DATABASE.get(0).getDescription());
        Assert.assertEquals(TodoPriority.HIGH, DATABASE.get(0).getPriority());
        Assert.assertEquals(TodoStatus.NOT_COMPLETED, DATABASE.get(0).getStatus());
    }
}
