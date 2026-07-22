package com.epitech.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param ownerId provisoire : tant que l'authentification n'existe pas (module 4),
 *                le proprietaire doit etre fourni par l'appelant. Il sera ensuite
 *                deduit du jeton JWT et ce champ disparaitra.
 */
public record CreateProjectRequest(

        @NotBlank(message = "Le nom du projet est obligatoire")
        @Size(max = 120, message = "Le nom ne peut pas depasser 120 caracteres")
        String name,

        @Size(max = 2000, message = "La description ne peut pas depasser 2000 caracteres")
        String description,

        @NotNull(message = "Le proprietaire est obligatoire")
        Long ownerId
) {
}
