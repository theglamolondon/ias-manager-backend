package net.ivoireautoservice.ias_manager.controller;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.dto.AuthenticationRequest;
import net.ivoireautoservice.ias_manager.auth.dto.AuthenticationResponse;
import net.ivoireautoservice.ias_manager.auth.JwtService;
import net.ivoireautoservice.ias_manager.auth.UtilisateurDetailsService;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
            )
        );

        final Utilisateur user = (Utilisateur) auth.getPrincipal();
		assert user != null;
		final String jwt = jwtService.generateToken(user);
        return ResponseEntity.ok(buildResponse(jwt, user));
    }

    @GetMapping("/info")
    public ResponseEntity<AuthenticationResponse> info(@AuthenticationPrincipal Utilisateur user) {
        return ResponseEntity.ok(buildResponse(null, user));
    }

    private AuthenticationResponse buildResponse(String token, Utilisateur user) {
        String role = user.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
        return new AuthenticationResponse(token, user.getNom(), user.getPrenom(), user.getEmail(), role);
    }
}
