package com.tbot.telegrambot.db.repository;

import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Set;

/**
 * @author Vladislav Marchenko
 */
public interface TodoRepository extends JpaRepository<TodoEntity, Integer> {
    Set<TodoEntity> findAllByUser(int user);

    Set<TodoEntity> findAllByUserAndStatus(int user, TodoStatus status);

    Set<TodoEntity> findAllByUserAndDue(int user, Date due);
}
