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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public List<TaskResponse> findByProject(Long projectId) {
        requireProjectExists(projectId);
        return taskRepository.findByProjectIdWithAssignee(projectId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse findById(Long projectId, Long taskId) {
        requireProjectExists(projectId);
        return TaskResponse.from(getTaskOrThrow(projectId, taskId));
    }

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.project(projectId));

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .dueDate(request.dueDate())
                .project(project)
                .assignee(resolveAssignee(request.assigneeId()))
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long projectId, Long taskId, UpdateTaskRequest request) {
        requireProjectExists(projectId);
        Task task = getTaskOrThrow(projectId, taskId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setAssignee(resolveAssignee(request.assigneeId()));

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long projectId, Long taskId) {
        requireProjectExists(projectId);
        taskRepository.delete(getTaskOrThrow(projectId, taskId));
    }

    /**
     * Le module 5 completera cette resolution par une verification d'appartenance :
     * on ne doit pouvoir assigner une tache qu'a un membre du projet concerne.
     */
    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> ResourceNotFoundException.user(assigneeId));
    }

    /**
     * Distingue un projet inexistant (404 sur le projet) d'une tache absente de ce projet,
     * au lieu de renvoyer une erreur identique dans les deux cas.
     */
    private void requireProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw ResourceNotFoundException.project(projectId);
        }
    }

    private Task getTaskOrThrow(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> ResourceNotFoundException.task(taskId, projectId));
    }
}
