package com.tbot.telegrambot.db.repository;

import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Set;


public interface TodoRepository extends JpaRepository<TodoEntity, Integer> {
    TodoEntity findById(int id);

    Set<TodoEntity> findAllByUserIDOrderByIdAsc(long user);

    Set<TodoEntity> findAllByUserIDAndStatusOrderByIdAsc(long user, TodoStatus status);

    Set<TodoEntity> findAllByUserIDAndDueOrderByIdAsc(long user, Date due);
}
