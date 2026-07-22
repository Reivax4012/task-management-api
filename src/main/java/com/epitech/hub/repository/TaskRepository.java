package com.epitech.hub.repository;

import com.epitech.hub.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** {@code LEFT JOIN} sur l'assigne : une tache non assignee doit rester dans les resultats. */
    @Query("""
            SELECT t FROM Task t
            LEFT JOIN FETCH t.assignee
            WHERE t.project.id = :projectId
            ORDER BY t.createdAt DESC
            """)
    List<Task> findByProjectIdWithAssignee(Long projectId);

    /**
     * Recherche par identifiant <b>et</b> par projet : une tache ne doit jamais etre
     * atteignable via l'URL d'un autre projet que le sien.
     */
    @Query("""
            SELECT t FROM Task t
            LEFT JOIN FETCH t.assignee
            WHERE t.id = :taskId AND t.project.id = :projectId
            """)
    Optional<Task> findByIdAndProjectId(Long taskId, Long projectId);

    /**
     * Suppression en masse, sans charger les taches en memoire : un projet peut en
     * compter des milliers.
     * <p>
     * {@code clearAutomatically} vide le contexte de persistance apres coup, une requete
     * de ce type contournant le cache de premier niveau : sans cela, une lecture
     * ulterieure dans la meme transaction renverrait des taches pourtant supprimees.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Task t WHERE t.project.id = :projectId")
    void deleteByProjectId(Long projectId);
}
