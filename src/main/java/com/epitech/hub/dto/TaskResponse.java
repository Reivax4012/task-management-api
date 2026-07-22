package com.epitech.hub.dto;

import com.epitech.hub.entity.Task;
import com.epitech.hub.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Long projectId,
        UserSummary assignee,
        Instant createdAt,
        Instant updatedAt
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getProject().getId(),
                UserSummary.from(task.getAssignee()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
