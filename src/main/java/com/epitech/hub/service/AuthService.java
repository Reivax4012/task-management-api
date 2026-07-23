package com.epitech.hub.service;

import com.epitech.hub.dto.AuthResponse;
import com.epitech.hub.dto.LoginRequest;
import com.epitech.hub.dto.RegisterRequest;
import com.epitech.hub.entity.User;
import com.epitech.hub.exception.DuplicateResourceException;
import com.epitech.hub.repository.UserRepository;
import com.epitech.hub.security.JwtService;
import com.epitech.hub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Cree un compte et renvoie immediatement un jeton, evitant a l'utilisateur un
     * appel de connexion juste apres son inscription.
     * <p>
     * Le mot de passe n'est jamais stocke : seul son hash BCrypt est persiste. L'unicite
     * est verifiee en amont pour renvoyer un message clair, la contrainte en base
     * restant le garde-fou ultime en cas de creations concurrentes.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Un compte existe deja pour cet email");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Ce nom d'utilisateur est deja pris");
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token, user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * Verifie les identifiants via l'AuthenticationManager (qui compare le hash BCrypt)
     * et delivre un jeton. Toute erreur remonte en AuthenticationException, traduite en
     * 401 au message neutre par le gestionnaire global.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.id(), principal.email());
        return AuthResponse.bearer(token, principal.id(), principal.displayName(), principal.email());
    }
}
