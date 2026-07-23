package com.epitech.hub.controller;

import com.epitech.hub.dto.AssignTaskRequest;
import com.epitech.hub.dto.ChangeStatusRequest;
import com.epitech.hub.dto.CreateTaskRequest;
import com.epitech.hub.dto.TaskResponse;
import com.epitech.hub.dto.UpdateTaskRequest;
import com.epitech.hub.security.UserPrincipal;
import com.epitech.hub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Les taches sont exposees sous leur projet : une tache n'existe pas hors d'un projet,
 * et cette imbrication rend naturelles les verifications d'appartenance appliquees par
 * le service a chaque action.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponse> list(@PathVariable Long projectId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.findByProject(projectId, principal.id());
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable Long projectId, @PathVariable Long taskId,
                            @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.findById(projectId, taskId, principal.id());
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId,
                                               @Valid @RequestBody CreateTaskRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               UriComponentsBuilder uriBuilder) {
        TaskResponse created = taskService.create(projectId, request, principal.id());
        return ResponseEntity
                .created(uriBuilder.path("/api/projects/{projectId}/tasks/{taskId}")
                        .build(projectId, created.id()))
                .body(created);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long projectId,
                               @PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.update(projectId, taskId, request, principal.id());
    }

    /** Route metier dediee : changer le statut sans reecrire toute la tache. */
    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long projectId,
                                     @PathVariable Long taskId,
                                     @Valid @RequestBody ChangeStatusRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.changeStatus(projectId, taskId, request.status(), principal.id());
    }

    /** Route metier dediee : assigner ou desassigner la tache a un membre du projet. */
    @PatchMapping("/{taskId}/assignee")
    public TaskResponse assign(@PathVariable Long projectId,
                               @PathVariable Long taskId,
                               @RequestBody AssignTaskRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.assign(projectId, taskId, request.assigneeId(), principal.id());
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long taskId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        taskService.delete(projectId, taskId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
