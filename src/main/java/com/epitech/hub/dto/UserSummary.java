package com.epitech.hub.dto;

import com.epitech.hub.entity.User;

/**
 * Vue minimale d'un utilisateur, imbriquee dans les autres reponses.
 * Elle n'expose ni l'email ni le hash du mot de passe.
 */
public record UserSummary(Long id, String username) {

    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getUsername());
    }
}
