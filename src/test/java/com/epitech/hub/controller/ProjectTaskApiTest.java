package com.epitech.hub.controller;

import com.epitech.hub.entity.User;
import com.epitech.hub.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration traversant toute la pile : HTTP, validation, service, JPA, H2.
 * {@code @Transactional} annule les ecritures apres chaque test, qui reste ainsi isole.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectTaskApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    private Long ownerId;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("owner@epitech.eu")
                .username("owner")
                .passwordHash("$2a$10$fakehashfortestingpurposesonly000000000000000000000000")
                .build());
        ownerId = owner.getId();
    }

    private Long createProject(String name) throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("name", name)
                .put("description", "Description")
                .put("ownerId", ownerId);

        String json = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/projects cree un projet et renvoie 201 + Location")
    void createProject() throws Exception {
        Long id = createProject("Projet HUB");

        mockMvc.perform(get("/api/projects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projet HUB"))
                .andExpect(jsonPath("$.owner.username").value("owner"))
                // le hash du mot de passe ne doit jamais transiter par l'API
                .andExpect(jsonPath("$.owner.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/projects sans nom renvoie 400 et detaille le champ fautif")
    void createProjectRejectsBlankName() throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("name", "")
                .put("ownerId", ownerId);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("GET /api/projects/{id} inconnu renvoie 404 au format ProblemDetail")
    void unknownProjectReturns404() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    @DisplayName("GET /api/projects/abc renvoie 400 et non une erreur serveur")
    void malformedIdReturns400() throws Exception {
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parametre invalide"));
    }

    @Test
    @DisplayName("PUT puis DELETE d'un projet")
    void updateThenDeleteProject() throws Exception {
        Long id = createProject("Nom initial");

        ObjectNode update = objectMapper.createObjectNode()
                .put("name", "Nom modifie")
                .put("description", "Nouvelle description");

        mockMvc.perform(put("/api/projects/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nom modifie"));

        mockMvc.perform(delete("/api/projects/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cycle de vie complet d'une tache dans son projet")
    void taskLifecycle() throws Exception {
        Long projectId = createProject("Projet avec taches");

        ObjectNode taskBody = objectMapper.createObjectNode()
                .put("title", "Ecrire la documentation")
                .put("dueDate", "2026-09-15");

        String created = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(created).get("id").asLong();

        ObjectNode update = objectMapper.createObjectNode()
                .put("title", "Documentation relue")
                .put("status", "DONE");
        update.put("assigneeId", ownerId);

        mockMvc.perform(put("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.assignee.username").value("owner"));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("une tache n'est pas accessible via l'URL d'un autre projet")
    void taskNotReachableFromAnotherProject() throws Exception {
        Long projectA = createProject("Projet A");
        Long projectB = createProject("Projet B");

        ObjectNode taskBody = objectMapper.createObjectNode().put("title", "Tache du projet A");
        String created = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/projects/{projectId}/tasks/{taskId}", projectB, taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("supprimer un projet supprime aussi ses taches restantes")
    void deletingProjectRemovesItsTasks() throws Exception {
        Long projectId = createProject("Projet a supprimer");

        for (String title : new String[]{"Tache 1", "Tache 2"}) {
            mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.createObjectNode().put("title", title).toString()))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(delete("/api/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un statut de tache inconnu renvoie 400")
    void unknownStatusReturns400() throws Exception {
        Long projectId = createProject("Projet");

        ObjectNode body = objectMapper.createObjectNode()
                .put("title", "Tache")
                .put("status", "STATUT_INEXISTANT");

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest());
    }
}
