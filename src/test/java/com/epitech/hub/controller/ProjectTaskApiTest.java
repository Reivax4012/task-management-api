package com.epitech.hub.controller;

import com.epitech.hub.entity.User;
import com.epitech.hub.repository.UserRepository;
import com.epitech.hub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration traversant toute la pile : filtre JWT, HTTP, validation, service,
 * JPA, H2. Chaque requete porte un vrai jeton, le filtre de securite est donc lui aussi
 * exerce. {@code @Transactional} annule les ecritures apres chaque test, qui reste isole.
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
    @Autowired
    private JwtService jwtService;

    private Long ownerId;
    private String token;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("owner@epitech.eu")
                .username("owner")
                .passwordHash("$2a$10$fakehashfortestingpurposesonly000000000000000000000000")
                .build());
        ownerId = owner.getId();
        token = jwtService.generateToken(owner.getId(), owner.getEmail());
    }

    /** Attache le jeton Bearer a une requete, comme le ferait un client authentifie. */
    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private Long createProject(String name) throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("name", name)
                .put("description", "Description");

        String json = mockMvc.perform(authed(post("/api/projects"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/projects cree un projet detenu par l'utilisateur authentifie")
    void createProject() throws Exception {
        Long id = createProject("Projet HUB");

        mockMvc.perform(authed(get("/api/projects/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projet HUB"))
                .andExpect(jsonPath("$.owner.id").value(ownerId))
                .andExpect(jsonPath("$.owner.username").value("owner"))
                // le hash du mot de passe ne doit jamais transiter par l'API
                .andExpect(jsonPath("$.owner.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/projects sans nom renvoie 400 et detaille le champ fautif")
    void createProjectRejectsBlankName() throws Exception {
        ObjectNode body = objectMapper.createObjectNode().put("name", "");

        mockMvc.perform(authed(post("/api/projects"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("sans jeton, les routes projet renvoient 401")
    void projectsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Non authentifie"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/projects/{id} inconnu renvoie 404 au format ProblemDetail")
    void unknownProjectReturns404() throws Exception {
        mockMvc.perform(authed(get("/api/projects/{id}", 999999)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    @DisplayName("GET /api/projects/abc renvoie 400 et non une erreur serveur")
    void malformedIdReturns400() throws Exception {
        mockMvc.perform(authed(get("/api/projects/abc")))
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

        mockMvc.perform(authed(put("/api/projects/{id}", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nom modifie"));

        mockMvc.perform(authed(delete("/api/projects/{id}", id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get("/api/projects/{id}", id)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cycle de vie complet d'une tache dans son projet")
    void taskLifecycle() throws Exception {
        Long projectId = createProject("Projet avec taches");

        ObjectNode taskBody = objectMapper.createObjectNode()
                .put("title", "Ecrire la documentation")
                .put("dueDate", "2026-09-15");

        String created = mockMvc.perform(authed(post("/api/projects/{projectId}/tasks", projectId))
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

        mockMvc.perform(authed(put("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.assignee.username").value("owner"));

        mockMvc.perform(authed(get("/api/projects/{projectId}/tasks", projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(authed(delete("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("une tache n'est pas accessible via l'URL d'un autre projet que le sien")
    void taskNotReachableFromAnotherProject() throws Exception {
        Long projectA = createProject("Projet A");
        Long projectB = createProject("Projet B");

        ObjectNode taskBody = objectMapper.createObjectNode().put("title", "Tache du projet A");
        String created = mockMvc.perform(authed(post("/api/projects/{projectId}/tasks", projectA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(authed(get("/api/projects/{projectId}/tasks/{taskId}", projectB, taskId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("supprimer un projet supprime aussi ses taches restantes")
    void deletingProjectRemovesItsTasks() throws Exception {
        Long projectId = createProject("Projet a supprimer");

        for (String title : new String[]{"Tache 1", "Tache 2"}) {
            mockMvc.perform(authed(post("/api/projects/{projectId}/tasks", projectId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.createObjectNode().put("title", title).toString()))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(authed(delete("/api/projects/{id}", projectId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get("/api/projects/{id}", projectId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(authed(get("/api/projects/{projectId}/tasks", projectId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un statut de tache inconnu renvoie 400")
    void unknownStatusReturns400() throws Exception {
        Long projectId = createProject("Projet");

        ObjectNode body = objectMapper.createObjectNode()
                .put("title", "Tache")
                .put("status", "STATUT_INEXISTANT");

        mockMvc.perform(authed(post("/api/projects/{projectId}/tasks", projectId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest());
    }
}
