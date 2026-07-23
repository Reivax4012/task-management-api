package com.epitech.hub.security;

import com.epitech.hub.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptateur entre l'entite {@link User} et le contrat {@link UserDetails} de Spring
 * Security. Il porte l'identifiant technique en plus des informations d'authentification,
 * ce qui permet aux controleurs de recuperer l'utilisateur courant sans relire la base.
 * <p>
 * Aucun role global n'est expose ici : dans cette application, les autorisations
 * dependent du projet (module 6) et non d'un role porte par le compte.
 *
 * @param id           identifiant technique de l'utilisateur.
 * @param email        identifiant de connexion, renvoye par {@code getUsername}.
 * @param displayName  nom d'affichage (le {@code username} de l'entite), distinct de
 *                     l'email : {@code getUsername} de Spring Security designe ici l'email.
 * @param passwordHash hash BCrypt, compare par Spring Security a la connexion.
 */
public record UserPrincipal(Long id, String email, String displayName, String passwordHash)
        implements UserDetails {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getUsername(),
                user.getPasswordHash());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
