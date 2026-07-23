package com.epitech.hub.dto;

import com.epitech.hub.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Corps de la route dediee au changement de statut d'une tache. */
public record ChangeStatusRequest(

        @NotNull(message = "Le statut est obligatoire")
        TaskStatus status
) {
}
