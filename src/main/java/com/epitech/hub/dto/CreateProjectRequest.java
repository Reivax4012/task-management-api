package com.epitech.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Le proprietaire n'est plus fourni par le client : il est deduit du jeton JWT de
 * l'appelant (module 4). Un utilisateur ne peut donc creer un projet qu'en son propre nom.
 */
public record CreateProjectRequest(

        @NotBlank(message = "Le nom du projet est obligatoire")
        @Size(max = 120, message = "Le nom ne peut pas depasser 120 caracteres")
        String name,

        @Size(max = 2000, message = "La description ne peut pas depasser 2000 caracteres")
        String description
) {
}
