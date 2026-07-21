package com.epitech.hub.entity;

import com.epitech.hub.config.JpaConfig;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifie le mapping JPA du module 2 : relations, contraintes et audit.
 * <p>
 * {@code @Import(JpaConfig.class)} est necessaire car {@code @DataJpaTest} ne charge pas
 * les {@code @Configuration} applicatives : sans lui, l'audit serait inactif et les
 * colonnes {@code created_at} (non nulles) resteraient vides.
 */
@DataJpaTest
@Import(JpaConfig.class)
class EntityMappingTest {

    @Autowired
    private TestEntityManager em;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = em.persistAndFlush(User.builder()
                .email("xavier@epitech.eu")
                .username("xavier")
                .passwordHash("$2a$10$fakehashfortestingpurposesonly000000000000000000000000")
                .build());

        project = em.persistAndFlush(Project.builder()
                .name("Projet HUB")
                .description("API de gestion de taches")
                .owner(owner)
                .build());
    }

    @Test
    @DisplayName("l'audit renseigne createdAt et updatedAt automatiquement")
    void auditingFieldsArePopulated() {
        assertThat(owner.getCreatedAt()).isNotNull();
        assertThat(project.getCreatedAt()).isNotNull();
        assertThat(project.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("une tache est rattachee a son projet et a son assigne")
    void taskLinksProjectAndAssignee() {
        Task task = em.persistAndFlush(Task.builder()
                .title("Ecrire les entites JPA")
                .project(project)
                .assignee(owner)
                .dueDate(LocalDate.of(2026, 8, 1))
                .build());

        em.clear();
        Task reloaded = em.find(Task.class, task.getId());

        assertThat(reloaded.getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getAssignee().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("le statut par defaut d'une tache est TODO")
    void taskDefaultsToTodo() {
        Task task = em.persistAndFlush(Task.builder()
                .title("Tache sans statut explicite")
                .project(project)
                .build());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    @DisplayName("le statut est persiste sous forme de chaine, pas d'ordinal")
    void statusIsStoredAsString() {
        Task task = em.persistAndFlush(Task.builder()
                .title("Tache en cours")
                .status(TaskStatus.IN_PROGRESS)
                .project(project)
                .build());

        Object stored = em.getEntityManager()
                .createNativeQuery("SELECT status FROM tasks WHERE id = :id")
                .setParameter("id", task.getId())
                .getSingleResult();

        assertThat(stored).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("une adhesion porte le role de l'utilisateur sur le projet")
    void membershipCarriesRole() {
        ProjectMember member = em.persistAndFlush(ProjectMember.builder()
                .project(project)
                .user(owner)
                .role(Role.ADMIN)
                .build());

        em.clear();
        ProjectMember reloaded = em.find(ProjectMember.class, member.getId());

        assertThat(reloaded.getRole()).isEqualTo(Role.ADMIN);
        assertThat(reloaded.getJoinedAt()).isNotNull();
        assertThat(reloaded.getUser().getUsername()).isEqualTo("xavier");
    }

    @Test
    @DisplayName("un utilisateur ne peut pas avoir deux roles sur le meme projet")
    void duplicateMembershipIsRejected() {
        em.persistAndFlush(ProjectMember.builder()
                .project(project).user(owner).role(Role.ADMIN).build());

        assertThatThrownBy(() -> em.persistAndFlush(ProjectMember.builder()
                .project(project).user(owner).role(Role.VIEWER).build()))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("deux comptes ne peuvent pas partager le meme email")
    void duplicateEmailIsRejected() {
        assertThatThrownBy(() -> em.persistAndFlush(User.builder()
                .email("xavier@epitech.eu")
                .username("autre")
                .passwordHash("$2a$10$fakehashfortestingpurposesonly000000000000000000000000")
                .build()))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("supprimer un projet supprime en cascade ses taches et ses adhesions")
    void deletingProjectCascades() {
        project.getTasks().add(Task.builder()
                .title("Tache a supprimer").project(project).build());
        project.getMembers().add(ProjectMember.builder()
                .project(project).user(owner).role(Role.ADMIN).build());
        em.persistAndFlush(project);

        em.getEntityManager().remove(em.find(Project.class, project.getId()));
        em.flush();
        em.clear();

        Long remainingTasks = (Long) em.getEntityManager()
                .createQuery("SELECT COUNT(t) FROM Task t").getSingleResult();
        Long remainingMembers = (Long) em.getEntityManager()
                .createQuery("SELECT COUNT(m) FROM ProjectMember m").getSingleResult();

        assertThat(remainingTasks).isZero();
        assertThat(remainingMembers).isZero();
        // Le proprietaire, lui, survit a la suppression de son projet.
        assertThat(em.find(User.class, owner.getId())).isNotNull();
    }
}
