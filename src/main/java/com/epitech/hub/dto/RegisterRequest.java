package com.epitech.hub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        @Size(max = 180, message = "L'email ne peut pas depasser 180 caracteres")
        String email,

        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(min = 3, max = 60, message = "Le nom d'utilisateur doit faire entre 3 et 60 caracteres")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100, message = "Le mot de passe doit faire au moins 8 caracteres")
        String password
) {
}
