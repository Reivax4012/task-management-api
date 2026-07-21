# HUB — Task Management REST API

API REST de gestion de tâches collaborative (dans l'esprit d'un Jira simplifié), construite en Java / Spring Boot.
Projet Epitech — HUBER Xavier.

> Travail en cours. Ce README sera complété au module 11 (prérequis, lancement via Docker Compose, liste des endpoints, Swagger).

## Stack

- Java 21, Spring Boot 3.5
- Maven
- Spring Web, Spring Data JPA, Spring Security, Spring WebSocket (STOMP)
- PostgreSQL (runtime) / H2 (tests)
- JWT, BCrypt
- JUnit 5, Mockito, MockMvc

## Lancer le projet (dev)

```bash
mvn spring-boot:run
```

Puis vérifier : `GET http://localhost:8080/health` → `{"status":"UP"}`
