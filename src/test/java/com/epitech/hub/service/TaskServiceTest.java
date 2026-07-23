package com.epitech.hub.service;

import com.epitech.hub.dto.CreateTaskRequest;
import com.epitech.hub.dto.TaskResponse;
import com.epitech.hub.dto.UpdateTaskRequest;
import com.epitech.hub.entity.Project;
import com.epitech.hub.entity.Task;
import com.epitech.hub.entity.TaskStatus;
import com.epitech.hub.entity.User;
import com.epitech.hub.exception.ResourceNotFoundException;
import com.epitech.hub.repository.ProjectRepository;
import com.epitech.hub.repository.TaskRepository;
import com.epitech.hub.repository.UserRepository;
import com.epitech.hub.security.ProjectSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    /** Identifiant de l'utilisateur qui declenche les actions (un membre du projet). */
    private static final long CALLER = 10L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectSecurity projectSecurity;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private User assignee;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(1L).name("Projet HUB").build();
        assignee = User.builder().id(2L).username("alice").build();
    }

    @Test
    @DisplayName("une tache creee sans statut demarre a TODO")
    void createDefaultsToTodo() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = taskService.create(1L,
                new CreateTaskRequest("Ecrire les tests", null, null, null, null), CALLER);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.assignee()).isNull();
        verify(projectSecurity).requireMember(1L, CALLER);
    }

    @Test
    @DisplayName("une tache peut etre creee assignee a un membre et datee")
    void createWithAssigneeAndDueDate() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectSecurity.isMember(1L, 2L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = taskService.create(1L, new CreateTaskRequest(
                "Relire le module 3", "Description", TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 1), 2L), CALLER);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.assignee().username()).isEqualTo("alice");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("un non-membre ne peut pas creer de tache")
    void createDeniedForNonMember() {
        doThrow(new AccessDeniedException("non membre"))
                .when(projectSecurity).requireMember(1L, CALLER);

        assertThatThrownBy(() -> taskService.create(1L,
                new CreateTaskRequest("Tache", null, null, null, null), CALLER))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
        verify(projectRepository, never()).findById(any());
    }

    @Test
    @DisplayName("creer une tache dans un projet inexistant leve une erreur 404")
    void createFailsOnUnknownProject() {
        doThrow(ResourceNotFoundException.project(404L))
                .when(projectSecurity).requireMember(404L, CALLER);

        assertThatThrownBy(() -> taskService.create(404L,
                new CreateTaskRequest("Tache", null, null, null, null), CALLER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("assigner une tache a un utilisateur inexistant leve une erreur 404")
    void createFailsOnUnknownAssignee() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(1L,
                new CreateTaskRequest("Tache", null, null, null, 99L), CALLER))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("assigner une tache a un utilisateur non membre est refuse")
    void createFailsWhenAssigneeNotMember() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectSecurity.isMember(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(1L,
                new CreateTaskRequest("Tache", null, null, null, 2L), CALLER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("membre");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("mettre a jour avec assigneeId null desassigne la tache")
    void updateCanUnassign() {
        Task task = Task.builder()
                .id(5L).title("Ancien titre").status(TaskStatus.TODO)
                .project(project).assignee(assignee).build();
        when(taskRepository.findByIdAndProjectId(5L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.update(1L, 5L, new UpdateTaskRequest(
                "Nouveau titre", null, TaskStatus.DONE, null, null), CALLER);

        assertThat(response.assignee()).isNull();
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.title()).isEqualTo("Nouveau titre");
    }

    @Test
    @DisplayName("changeStatus fait avancer la tache dans son cycle de vie")
    void changeStatusUpdatesOnlyStatus() {
        Task task = Task.builder()
                .id(5L).title("Tache").status(TaskStatus.TODO).project(project).build();
        when(taskRepository.findByIdAndProjectId(5L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.changeStatus(1L, 5L, TaskStatus.IN_PROGRESS, CALLER);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.title()).isEqualTo("Tache");
        verify(projectSecurity).requireMember(1L, CALLER);
    }

    @Test
    @DisplayName("assign attribue la tache a un membre")
    void assignToMember() {
        Task task = Task.builder()
                .id(5L).title("Tache").status(TaskStatus.TODO).project(project).build();
        when(taskRepository.findByIdAndProjectId(5L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectSecurity.isMember(1L, 2L)).thenReturn(true);

        TaskResponse response = taskService.assign(1L, 5L, 2L, CALLER);

        assertThat(response.assignee().username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("assign a un non-membre est refuse")
    void assignToNonMemberIsDenied() {
        Task task = Task.builder()
                .id(5L).title("Tache").status(TaskStatus.TODO).project(project).build();
        when(taskRepository.findByIdAndProjectId(5L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(projectSecurity.isMember(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.assign(1L, 5L, 2L, CALLER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("une tache n'est pas atteignable via un autre projet que le sien")
    void taskIsScopedToItsProject() {
        when(taskRepository.findByIdAndProjectId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(2L, 5L, CALLER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tache 5");
    }

    @Test
    @DisplayName("lister les taches d'un projet inexistant leve une erreur 404")
    void listFailsOnUnknownProject() {
        doThrow(ResourceNotFoundException.project(404L))
                .when(projectSecurity).requireMember(404L, CALLER);

        assertThatThrownBy(() -> taskService.findByProject(404L, CALLER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Projet 404");
    }

    @Test
    @DisplayName("un non-membre ne peut pas lister les taches d'un projet")
    void listDeniedForNonMember() {
        doThrow(new AccessDeniedException("non membre"))
                .when(projectSecurity).requireMember(1L, CALLER);

        assertThatThrownBy(() -> taskService.findByProject(1L, CALLER))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).findByProjectIdWithAssignee(any());
    }
}
