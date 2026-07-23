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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regles metier des taches. Toute operation commence par verifier que l'appelant est
 * membre du projet concerne : une tache n'est accessible qu'a travers un projet auquel
 * on appartient.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectSecurity projectSecurity;

    public List<TaskResponse> findByProject(Long projectId, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        return taskRepository.findByProjectIdWithAssignee(projectId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse findById(Long projectId, Long taskId, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        return TaskResponse.from(getTaskOrThrow(projectId, taskId));
    }

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.project(projectId));

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .dueDate(request.dueDate())
                .project(project)
                .assignee(resolveAssignee(projectId, request.assigneeId()))
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long projectId, Long taskId, UpdateTaskRequest request, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        Task task = getTaskOrThrow(projectId, taskId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setAssignee(resolveAssignee(projectId, request.assigneeId()));

        return TaskResponse.from(task);
    }

    /** Operation metier dediee : fait avancer une tache dans son cycle de vie. */
    @Transactional
    public TaskResponse changeStatus(Long projectId, Long taskId, TaskStatus status, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        Task task = getTaskOrThrow(projectId, taskId);
        task.setStatus(status);
        return TaskResponse.from(task);
    }

    /** Operation metier dediee : (dé)assigne une tache a un membre du projet. */
    @Transactional
    public TaskResponse assign(Long projectId, Long taskId, Long assigneeId, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        Task task = getTaskOrThrow(projectId, taskId);
        task.setAssignee(resolveAssignee(projectId, assigneeId));
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long projectId, Long taskId, Long userId) {
        projectSecurity.requireMember(projectId, userId);
        taskRepository.delete(getTaskOrThrow(projectId, taskId));
    }

    /**
     * Resout l'assigne en garantissant qu'il est <b>membre du projet</b>, et pas seulement
     * un utilisateur existant : on n'assigne pas une tache a quelqu'un d'exterieur au projet.
     * Le non-membre est un refus d'acces (403), a distinguer d'un identifiant inconnu (404).
     */
    private User resolveAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> ResourceNotFoundException.user(assigneeId));
        if (!projectSecurity.isMember(projectId, assigneeId)) {
            throw new AccessDeniedException(
                    "L'utilisateur vise n'est pas membre de ce projet");
        }
        return assignee;
    }

    private Task getTaskOrThrow(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> ResourceNotFoundException.task(taskId, projectId));
    }
}
