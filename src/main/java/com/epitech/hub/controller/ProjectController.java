package com.epitech.hub.controller;

import com.epitech.hub.dto.CreateProjectRequest;
import com.epitech.hub.dto.ProjectResponse;
import com.epitech.hub.dto.UpdateProjectRequest;
import com.epitech.hub.security.UserPrincipal;
import com.epitech.hub.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return projectService.findById(id);
    }

    /**
     * Repond 201 avec l'en-tete Location pointant vers la ressource creee.
     * Le proprietaire est l'utilisateur authentifie, injecte depuis le contexte de securite.
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal,
                                                  UriComponentsBuilder uriBuilder) {
        ProjectResponse created = projectService.create(request, principal.id());
        return ResponseEntity
                .created(uriBuilder.path("/api/projects/{id}").build(created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
