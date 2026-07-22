package com.epitech.hub.dto;

import com.epitech.hub.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * @param status     optionnel : une tache creee sans statut demarre a TODO.
 * @param assigneeId optionnel : une tache peut naitre sans assigne.
 */
public record CreateTaskRequest(

        @NotBlank(message = "Le titre de la tache est obligatoire")
        @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
        String title,

        @Size(max = 5000, message = "La description ne peut pas depasser 5000 caracteres")
        String description,

        TaskStatus status,

        LocalDate dueDate,

        Long assigneeId
) {
}
