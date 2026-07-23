package com.epitech.hub.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests unitaires purs du service JWT : ni contexte Spring, ni base de donnees. */
class JwtServiceTest {

    private static final String SECRET = "un-secret-de-test-suffisamment-long-pour-hs256-0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 3_600_000L));
    }

    @Test
    @DisplayName("un jeton genere se relit avec le meme email et le meme identifiant")
    void roundTrip() {
        String token = jwtService.generateToken(42L, "xavier@epitech.eu");

        Claims claims = jwtService.parse(token).orElseThrow();
        assertThat(jwtService.extractEmail(claims)).isEqualTo("xavier@epitech.eu");
        assertThat(jwtService.extractUserId(claims)).isEqualTo(42L);
    }

    @Test
    @DisplayName("un jeton signe avec une autre cle est rejete")
    void rejectsTokenSignedWithAnotherKey() {
        JwtService attacker = new JwtService(
                new JwtProperties("une-cle-totalement-differente-mais-aussi-longue-9876", 3_600_000L));
        String forged = attacker.generateToken(1L, "pirate@epitech.eu");

        assertThat(jwtService.parse(forged)).isEmpty();
    }

    @Test
    @DisplayName("un jeton deja expire est rejete")
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, -1_000L));
        String expired = shortLived.generateToken(1L, "xavier@epitech.eu");

        assertThat(jwtService.parse(expired)).isEmpty();
    }

    @Test
    @DisplayName("une chaine qui n'est pas un JWT est rejetee sans lever d'exception")
    void rejectsGarbage() {
        Optional<Claims> result = jwtService.parse("ceci-nest-pas-un-jeton");
        assertThat(result).isEmpty();
    }
}
