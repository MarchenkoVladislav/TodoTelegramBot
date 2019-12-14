package com.tbot.telegrambot.db.repository;

import com.tbot.telegrambot.db.entity.TodoEntity;
import com.tbot.telegrambot.db.enums.TodoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Set;


public interface TodoRepository extends JpaRepository<TodoEntity, Integer> {
    boolean existsById(int id);

    TodoEntity findById(int id);

    Set<TodoEntity> findAllByUserIdOrderByIdAsc(long user);

    Set<TodoEntity> findAllByUserIdAndStatusOrderByIdAsc(long user, TodoStatus status);

    Set<TodoEntity> findAllByUserIdAndDueDateOrderByIdAsc(long user, Date due);
}
