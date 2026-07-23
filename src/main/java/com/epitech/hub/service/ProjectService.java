package com.epitech.hub.service;

import com.epitech.hub.dto.CreateProjectRequest;
import com.epitech.hub.dto.ProjectResponse;
import com.epitech.hub.dto.UpdateProjectRequest;
import com.epitech.hub.entity.Project;
import com.epitech.hub.entity.ProjectMember;
import com.epitech.hub.entity.Role;
import com.epitech.hub.entity.User;
import com.epitech.hub.exception.ResourceNotFoundException;
import com.epitech.hub.repository.ProjectMemberRepository;
import com.epitech.hub.repository.ProjectRepository;
import com.epitech.hub.repository.TaskRepository;
import com.epitech.hub.repository.UserRepository;
import com.epitech.hub.security.ProjectSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectSecurity projectSecurity;

    /** Liste les projets dont l'utilisateur est membre, et eux seuls. */
    public List<ProjectResponse> findAllForUser(Long userId) {
        return projectRepository.findAllForMember(userId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    public ProjectResponse findById(Long id, Long userId) {
        projectSecurity.requireMember(id, userId);
        return ProjectResponse.from(getProjectOrThrow(id));
    }

    /**
     * Cree le projet et inscrit immediatement son proprietaire comme membre ADMIN :
     * sans cela, le createur n'aurait aucun droit sur son propre projet une fois les
     * permissions activees (module 6).
     */
    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> ResourceNotFoundException.user(ownerId));

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .build();

        // addMember tient les deux cotes de la relation a jour ; la cascade sur la
        // collection se charge d'inserer l'adhesion lors du save du projet.
        project.addMember(ProjectMember.builder()
                .user(owner)
                .role(Role.ADMIN)
                .build());

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request, Long userId) {
        projectSecurity.requireMember(id, userId);
        Project project = getProjectOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());
        // Entite geree par la transaction : Hibernate detecte la modification et
        // emet l'UPDATE au flush, un save() explicite serait redondant.
        return ProjectResponse.from(project);
    }

    /**
     * Supprime explicitement les taches puis les adhesions avant le projet.
     * <p>
     * S'en remettre a la seule cascade JPA serait fragile : elle ne s'applique qu'aux
     * elements presents dans les collections en memoire, et laisserait donc des lignes
     * orphelines des qu'une tache a ete creee par une autre transaction. La suppression
     * en masse ne depend, elle, d'aucun etat charge.
     */
    @Transactional
    public void delete(Long id, Long userId) {
        projectSecurity.requireMember(id, userId);
        taskRepository.deleteByProjectId(id);
        projectMemberRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findByIdWithOwner(id)
                .orElseThrow(() -> ResourceNotFoundException.project(id));
    }
}
