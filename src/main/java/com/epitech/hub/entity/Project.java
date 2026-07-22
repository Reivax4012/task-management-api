package com.epitech.hub.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Espace de travail regroupant des taches et des membres.
 * Supprimer un projet supprime en cascade ses taches et ses adhesions.
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    /**
     * Createur du projet. Il recoit le role {@link Role#ADMIN} a la creation (module 5),
     * mais reste distinct de la notion de membre : le proprietaire est une donnee d'audit.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectMember> members = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Rattache une adhesion en tenant <b>les deux cotes</b> de la relation a jour.
     * <p>
     * Enregistrer un ProjectMember directement par son depot laisserait cette liste
     * vide en memoire : la cascade porte sur la collection, elle ne supprimerait donc
     * pas l'adhesion a la suppression du projet, et le flush echouerait sur une ligne
     * orpheline referencant un projet disparu.
     */
    public void addMember(ProjectMember member) {
        members.add(member);
        member.setProject(this);
    }

    /** Meme raison que {@link #addMember(ProjectMember)}. */
    public void addTask(Task task) {
        tasks.add(task);
        task.setProject(this);
    }
}
