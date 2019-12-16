package com.tbot.telegrambot.db.entity;

import com.tbot.telegrambot.db.enums.TodoPriority;
import com.tbot.telegrambot.db.enums.TodoStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Entity
@Table(name = "todo_entity")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TodoEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "userId", nullable = false)
    private Long userId;

    @Column(name = "text", nullable = false)
    private String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TodoStatus status;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private TodoPriority priority;

    @Column(name = "dueDate", nullable = false)
    private Date dueDate;

    @Override
    public String toString() {
        String s = "Task #" + id +
                ":\n" + description +
                "\n- status - " + status.toString().replace("_", " ") +
                "\n- priority - " + priority +
                "\n- due date - " + dueDate.toString().substring(0, 10);

        if (status == TodoStatus.NOT_COMPLETED) {
            long diff = dueDate.getTime() - new Date().getTime();
            long days = Math.abs(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
            if (diff > 0) {
                s += "\n- days left - " + days;
            } else if (diff < 0) {
                s += "\n- expired " + days + " days ago";
            }
        }

        s += "\n\n";
        return s;
    }
}
