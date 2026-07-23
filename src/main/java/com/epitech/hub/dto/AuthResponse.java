package com.epitech.hub.dto;

/**
 * Renvoyee par l'inscription comme par la connexion.
 *
 * @param tokenType toujours « Bearer » : rappelle au client la forme attendue de
 *                  l'en-tete Authorization.
 */
public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String email
) {

    public static AuthResponse bearer(String token, Long userId, String username, String email) {
        return new AuthResponse(token, "Bearer", userId, username, email);
    }
}
