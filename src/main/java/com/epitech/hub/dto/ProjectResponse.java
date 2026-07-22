package com.epitech.hub.dto;

import com.epitech.hub.entity.Project;

import java.time.Instant;

/**
 * Les taches ne sont volontairement pas imbriquees ici : un projet peut en contenir
 * des milliers. Elles sont exposees par leur propre route paginee.
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        UserSummary owner,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                UserSummary.from(project.getOwner()),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
