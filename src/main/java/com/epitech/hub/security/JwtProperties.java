package com.epitech.hub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Regroupe les parametres JWT du fichier de configuration (prefixe {@code hub.jwt}).
 * Un record typé vaut mieux qu'une lecture eparse de {@code @Value} : la configuration
 * est validee et documentee en un seul endroit.
 *
 * @param secret       cle de signature HS256, au moins 256 bits.
 * @param expirationMs duree de validite du jeton, en millisecondes.
 */
@ConfigurationProperties(prefix = "hub.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
