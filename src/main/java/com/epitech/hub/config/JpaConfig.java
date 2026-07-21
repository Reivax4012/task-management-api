package com.epitech.hub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Active le remplissage automatique des champs {@code @CreatedDate} / {@code @LastModifiedDate}
 * des entites. Sans cette annotation, ces colonnes resteraient nulles et violeraient
 * leur contrainte {@code nullable = false}.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
