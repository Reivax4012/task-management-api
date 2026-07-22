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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;

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
                new CreateTaskRequest("Ecrire les tests", null, null, null, null));

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.assignee()).isNull();
    }

    @Test
    @DisplayName("une tache peut etre creee assignee et datee")
    void createWithAssigneeAndDueDate() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = taskService.create(1L, new CreateTaskRequest(
                "Relire le module 3", "Description", TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 1), 2L));

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.assignee().username()).isEqualTo("alice");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("creer une tache dans un projet inexistant leve une erreur")
    void createFailsOnUnknownProject() {
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(404L,
                new CreateTaskRequest("Tache", null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("assigner une tache a un utilisateur inexistant leve une erreur")
    void createFailsOnUnknownAssignee() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(1L,
                new CreateTaskRequest("Tache", null, null, null, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("mettre a jour avec assigneeId null desassigne la tache")
    void updateCanUnassign() {
        Task task = Task.builder()
                .id(5L).title("Ancien titre").status(TaskStatus.TODO)
                .project(project).assignee(assignee).build();
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findByIdAndProjectId(5L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.update(1L, 5L, new UpdateTaskRequest(
                "Nouveau titre", null, TaskStatus.DONE, null, null));

        assertThat(response.assignee()).isNull();
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.title()).isEqualTo("Nouveau titre");
    }

    @Test
    @DisplayName("une tache n'est pas atteignable via un autre projet que le sien")
    void taskIsScopedToItsProject() {
        when(projectRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findByIdAndProjectId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(2L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tache 5");
    }

    @Test
    @DisplayName("lister les taches d'un projet inexistant leve une erreur")
    void listFailsOnUnknownProject() {
        when(projectRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.findByProject(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Projet 404");
    }
}
