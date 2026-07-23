package com.epitech.hub.config;

import com.epitech.hub.security.AppUserDetailsService;
import com.epitech.hub.security.JwtAuthenticationFilter;
import com.epitech.hub.security.JwtProperties;
import com.epitech.hub.security.RestAccessDeniedHandler;
import com.epitech.hub.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de Spring Security pour une API REST protegee par JWT.
 * <p>
 * Points structurants :
 * <ul>
 *   <li>sessions <b>STATELESS</b> : l'etat d'authentification vit dans le jeton, pas
 *       dans une session serveur ; c'est ce qui rend l'API scalable horizontalement ;</li>
 *   <li>CSRF desactive : la protection CSRF vise les sessions par cookie, sans objet
 *       lorsqu'aucune session n'est utilisee ;</li>
 *   <li>le filtre JWT s'execute avant le filtre d'authentification par formulaire,
 *       afin d'alimenter le contexte de securite a partir du jeton.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Ouvert : inscription, connexion, sonde de sante et documentation.
                        .requestMatchers("/api/auth/**", "/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Tout le reste exige un jeton valide.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt : algorithme de hachage adaptatif recommande pour les mots de passe. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fournit l'AuthenticationManager utilise a la connexion. Le DaoAuthenticationProvider
     * relie le chargement des utilisateurs (par email) a la verification BCrypt du mot de passe.
     */
    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider::authenticate;
    }
}
