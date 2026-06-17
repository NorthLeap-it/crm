package it.northleap.backend.services;

import it.northleap.backend.dtos.LoginRequest;
import it.northleap.backend.dtos.LoginResponse;
import it.northleap.backend.entities.User;
import it.northleap.backend.repositories.UserRepository;
import it.northleap.backend.security.JwtService;
import it.northleap.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    // servizi + repo
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    // metodo per eseguire il login, usando dto request e response
    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // recupero l'utente
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        // genero il token
        String token = jwtService.generateToken(userPrincipal);

        return new LoginResponse(token, user.getId(), user.getEmail(), user.getName());

    }
}
