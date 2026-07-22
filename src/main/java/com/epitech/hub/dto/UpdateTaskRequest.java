package com.epitech.hub.dto;

import com.epitech.hub.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Remplacement complet de la tache (semantique PUT) : tout champ omis est ecrase.
 *
 * @param assigneeId null desassigne explicitement la tache.
 */
public record UpdateTaskRequest(

        @NotBlank(message = "Le titre de la tache est obligatoire")
        @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
        String title,

        @Size(max = 5000, message = "La description ne peut pas depasser 5000 caracteres")
        String description,

        @NotNull(message = "Le statut est obligatoire")
        TaskStatus status,

        LocalDate dueDate,

        Long assigneeId
) {
}
