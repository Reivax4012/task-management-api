package com.epitech.hub.security;

import com.epitech.hub.entity.ProjectMember;
import com.epitech.hub.exception.ResourceNotFoundException;
import com.epitech.hub.repository.ProjectMemberRepository;
import com.epitech.hub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Point unique de verification de l'acces a un projet. Toute action sur un projet ou
 * l'une de ses taches passe par ici pour garantir que l'appelant en est bien membre.
 * <p>
 * Le module 6 etendra ce composant avec des verifications de <i>role</i> (ADMIN, MEMBER,
 * VIEWER) et le referencera depuis des annotations {@code @PreAuthorize}. Le module 5 se
 * limite a l'appartenance : etre membre, quel que soit le role, suffit pour l'instant.
 */
@Component
@RequiredArgsConstructor
public class ProjectSecurity {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Exige que l'utilisateur soit membre du projet et renvoie son adhesion (porteuse du
     * role, utile au module 6).
     * <p>
     * Distingue deux cas : un projet inexistant renvoie 404, un projet existant dont
     * l'utilisateur n'est pas membre renvoie 403. Revendiquer un projet dont on n'est pas
     * membre est un refus d'acces, pas une absence de ressource.
     *
     * @throws ResourceNotFoundException si le projet n'existe pas.
     * @throws AccessDeniedException     si l'utilisateur n'est pas membre du projet.
     */
    @Transactional(readOnly = true)
    public ProjectMember requireMember(Long projectId, Long userId) {
        if (!projectRepository.existsById(projectId)) {
            throw ResourceNotFoundException.project(projectId);
        }
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Vous n'etes pas membre de ce projet"));
    }

    /** Variante booleenne, sans effet de bord, pour verifier une adhesion (ex. l'assigne). */
    @Transactional(readOnly = true)
    public boolean isMember(Long projectId, Long userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
