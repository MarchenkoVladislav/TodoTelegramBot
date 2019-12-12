package com.tbot.telegrambot.db.entity;

import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "todo_entities")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TodoEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "userID", nullable = false)
    private Long userID;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TodoStatus status;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private TodoPriority priority;

    @Column(name = "due", nullable = false)
    private Date due;

    @Override
    public String toString() {
        return "Task #" + id +
                ":\n" + text +
                "\nstatus - " + status +
                "\npriority - " + priority +
                "\ndue date - " + due.toString().substring(0,10) + "\n\n";
    }
}
