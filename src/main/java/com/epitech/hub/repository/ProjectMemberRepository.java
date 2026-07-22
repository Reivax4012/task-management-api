package com.epitech.hub.repository;

import com.epitech.hub.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @Query("""
            SELECT m FROM ProjectMember m
            JOIN FETCH m.user
            WHERE m.project.id = :projectId
            """)
    List<ProjectMember> findByProjectIdWithUser(Long projectId);

    /** Voir {@code TaskRepository#deleteByProjectId} pour le choix de la suppression en masse. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ProjectMember m WHERE m.project.id = :projectId")
    void deleteByProjectId(Long projectId);
}
