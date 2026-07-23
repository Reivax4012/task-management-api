package com.epitech.hub.controller;

import com.epitech.hub.entity.User;
import com.epitech.hub.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parcours d'authentification de bout en bout : inscription, connexion et leurs echecs. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private ObjectNode registerBody(String email, String username, String password) {
        return objectMapper.createObjectNode()
                .put("email", email)
                .put("username", username)
                .put("password", password);
    }

    @Test
    @DisplayName("l'inscription renvoie 201, un jeton, et hache le mot de passe")
    void registerReturnsTokenAndHashesPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("xavier@epitech.eu", "xavier", "motdepasse123").toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("xavier@epitech.eu"))
                .andExpect(jsonPath("$.username").value("xavier"));

        User saved = userRepository.findByEmail("xavier@epitech.eu").orElseThrow();
        // le mot de passe en clair ne doit jamais etre stocke
        assertThat(saved.getPasswordHash()).isNotEqualTo("motdepasse123");
        assertThat(passwordEncoder.matches("motdepasse123", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("s'inscrire avec un email deja pris renvoie 409")
    void registerDuplicateEmailReturns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("dup@epitech.eu", "premier", "motdepasse123").toString()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("dup@epitech.eu", "second", "motdepasse123").toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit"));
    }

    @Test
    @DisplayName("un email invalide ou un mot de passe trop court renvoie 400")
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("pas-un-email", "bob", "court").toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("la connexion avec les bons identifiants renvoie un jeton")
    void loginSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("login@epitech.eu", "loginuser", "motdepasse123").toString()))
                .andExpect(status().isCreated());

        ObjectNode login = objectMapper.createObjectNode()
                .put("email", "login@epitech.eu")
                .put("password", "motdepasse123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("loginuser"));
    }

    @Test
    @DisplayName("la connexion avec un mauvais mot de passe renvoie 401 au message neutre")
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("secure@epitech.eu", "secure", "motdepasse123").toString()))
                .andExpect(status().isCreated());

        ObjectNode login = objectMapper.createObjectNode()
                .put("email", "secure@epitech.eu")
                .put("password", "mauvais-mot-de-passe");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou mot de passe incorrect"));
    }

    @Test
    @DisplayName("se connecter a un compte inexistant renvoie le meme 401 neutre")
    void loginUnknownAccountReturnsSame401() throws Exception {
        ObjectNode login = objectMapper.createObjectNode()
                .put("email", "inconnu@epitech.eu")
                .put("password", "motdepasse123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou mot de passe incorrect"));
    }

    @Test
    @DisplayName("un jeton invalide sur une route protegee renvoie 401")
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer ceci.nest.pas.un.jeton")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }
}
