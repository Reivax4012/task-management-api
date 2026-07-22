package com.epitech.hub.exception;

/** Levee quand une ressource referencee par l'URL ou par le corps de la requete n'existe pas. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException project(Long id) {
        return new ResourceNotFoundException("Projet %d introuvable".formatted(id));
    }

    public static ResourceNotFoundException task(Long taskId, Long projectId) {
        return new ResourceNotFoundException(
                "Tache %d introuvable dans le projet %d".formatted(taskId, projectId));
    }

    public static ResourceNotFoundException user(Long id) {
        return new ResourceNotFoundException("Utilisateur %d introuvable".formatted(id));
    }
}
