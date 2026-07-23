package com.epitech.hub.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduit les exceptions en reponses HTTP coherentes pour toute l'API.
 * <p>
 * Le format retourne est {@link ProblemDetail} (RFC 7807), standard supporte nativement
 * par Spring Boot 3 : un client recoit toujours la meme structure, quelle que soit
 * l'erreur, plutot qu'une page d'erreur Whitelabel ou une trace Java.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    /**
     * Echec de connexion. Le message reste volontairement generique : preciser si c'est
     * l'email ou le mot de passe qui est faux permettrait d'enumerer les comptes existants.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Echec de l'authentification",
                "Email ou mot de passe incorrect");
    }

    /**
     * Utilisateur authentifie mais non autorise (ex. non membre du projet vise, ou role
     * insuffisant au module 6). Couvre aussi les refus des annotations {@code @PreAuthorize},
     * qui levent une sous-classe d'AccessDeniedException.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Acces refuse", ex.getMessage());
    }

    /** Declenchee par {@code @Valid} lorsqu'un champ du corps de la requete est invalide. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "Requete invalide",
                "Un ou plusieurs champs sont invalides");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /** JSON malforme, ou valeur d'enum inconnue (ex. un statut de tache inexistant). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Corps de requete illisible",
                "Le corps de la requete est absent, malforme, ou contient une valeur non reconnue");
    }

    /** Ex. /api/projects/abc alors qu'un identifiant numerique est attendu. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parametre invalide",
                "Le parametre '%s' n'a pas le format attendu".formatted(ex.getName()));
    }

    /**
     * Filet de securite : toute exception non prevue devient un 500 au message neutre.
     * Le detail reste dans les logs du serveur et n'est jamais renvoye au client,
     * une trace pouvant exposer la structure interne de l'application.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Exception non geree", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue");
    }

    private ProblemDetail build(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
