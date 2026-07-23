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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitaires : les depots sont simules, aucune base n'est demarree. */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("xavier").email("xavier@epitech.eu").build();
    }

    @Test
    @DisplayName("la creation inscrit le proprietaire comme membre ADMIN")
    void createRegistersOwnerAsAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(42L);
            return p;
        });

        ProjectResponse response = projectService.create(
                new CreateProjectRequest("Projet HUB", "Description"), 1L);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.owner().username()).isEqualTo("xavier");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        ProjectMember membership = captor.getValue().getMembers().getFirst();
        assertThat(membership.getRole()).isEqualTo(Role.ADMIN);
        assertThat(membership.getUser()).isEqualTo(owner);
    }

    @Test
    @DisplayName("l'adhesion creee reference bien son projet des les deux cotes")
    void createKeepsBothSidesOfRelationInSync() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));

        projectService.create(new CreateProjectRequest("Projet HUB", null), 1L);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();

        // Sans ce lien retour, la cascade ne supprimerait pas l'adhesion et la
        // suppression du projet echouerait sur une ligne orpheline.
        assertThat(saved.getMembers().getFirst().getProject()).isSameAs(saved);
    }

    @Test
    @DisplayName("la suppression retire taches et adhesions avant le projet")
    void deleteRemovesChildrenFirst() {
        when(projectRepository.existsById(7L)).thenReturn(true);

        projectService.delete(7L);

        InOrder inOrder = inOrder(taskRepository, projectMemberRepository, projectRepository);
        inOrder.verify(taskRepository).deleteByProjectId(7L);
        inOrder.verify(projectMemberRepository).deleteByProjectId(7L);
        inOrder.verify(projectRepository).deleteById(7L);
    }

    @Test
    @DisplayName("la creation echoue si le proprietaire n'existe pas")
    void createFailsOnUnknownOwner() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(
                new CreateProjectRequest("Projet", null), 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("la mise a jour modifie nom et description")
    void updateChangesFields() {
        Project project = Project.builder()
                .id(7L).name("Ancien nom").description("Ancienne").owner(owner).build();
        when(projectRepository.findByIdWithOwner(7L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.update(7L,
                new UpdateProjectRequest("Nouveau nom", "Nouvelle"));

        assertThat(response.name()).isEqualTo("Nouveau nom");
        assertThat(response.description()).isEqualTo("Nouvelle");
    }

    @Test
    @DisplayName("consulter un projet inexistant leve une erreur 404")
    void findByIdFailsWhenMissing() {
        when(projectRepository.findByIdWithOwner(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(123L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("123");
    }

    @Test
    @DisplayName("supprimer un projet inexistant leve une erreur 404")
    void deleteFailsWhenMissing() {
        when(projectRepository.existsById(123L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.delete(123L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).deleteByProjectId(any());
        verify(projectRepository, never()).deleteById(any());
    }
}
