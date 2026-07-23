package com.epitech.hub.repository;

import com.epitech.hub.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Charge le proprietaire dans la meme requete. Sans ce {@code JOIN FETCH}, afficher
     * une liste de N projets declencherait N requetes supplementaires pour resoudre
     * chaque proprietaire (probleme dit "N+1"), les relations etant en chargement paresseux.
     */
    @Query("SELECT p FROM Project p JOIN FETCH p.owner ORDER BY p.createdAt DESC")
    List<Project> findAllWithOwner();

    /**
     * Ne renvoie que les projets dont l'utilisateur est membre : la liste des projets est
     * personnelle, on n'expose pas a un utilisateur les projets auxquels il n'appartient pas.
     * La jointure sur {@code members} filtre, le {@code JOIN FETCH owner} evite le N+1.
     */
    @Query("""
            SELECT p FROM Project p
            JOIN FETCH p.owner
            JOIN p.members m
            WHERE m.user.id = :userId
            ORDER BY p.createdAt DESC
            """)
    List<Project> findAllForMember(Long userId);

    @Query("SELECT p FROM Project p JOIN FETCH p.owner WHERE p.id = :id")
    Optional<Project> findByIdWithOwner(Long id);
}
