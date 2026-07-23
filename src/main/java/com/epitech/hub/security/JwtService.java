package com.epitech.hub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Genere et valide les jetons JWT signes en HS256.
 * <p>
 * Le sujet du jeton est l'email de l'utilisateur, identifiant stable et unique servant
 * ensuite a recharger le compte. L'identifiant technique est place en claim afin d'eviter
 * une lecture en base a chaque requete lorsqu'on a seulement besoin de l'id.
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Valide la signature et l'expiration du jeton puis en extrait les claims.
     * <p>
     * Renvoie un {@link Optional} vide plutot que de propager l'exception : un jeton
     * invalide ou expire est un cas de fonctionnement normal (le filtre laissera alors
     * la requete non authentifiee), pas une erreur applicative.
     */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Jeton JWT rejete : {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    public Long extractUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Long.class);
    }
}
