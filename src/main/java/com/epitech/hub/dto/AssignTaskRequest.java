package com.epitech.hub.dto;

/**
 * Corps de la route dediee a l'assignation d'une tache.
 *
 * @param assigneeId identifiant du membre a qui assigner la tache ; {@code null} desassigne.
 */
public record AssignTaskRequest(Long assigneeId) {
}
