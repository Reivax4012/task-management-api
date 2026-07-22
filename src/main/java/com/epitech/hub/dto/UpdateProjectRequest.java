package com.epitech.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Le proprietaire d'un projet n'est pas modifiable via cette route. */
public record UpdateProjectRequest(

        @NotBlank(message = "Le nom du projet est obligatoire")
        @Size(max = 120, message = "Le nom ne peut pas depasser 120 caracteres")
        String name,

        @Size(max = 2000, message = "La description ne peut pas depasser 2000 caracteres")
        String description
) {
}
