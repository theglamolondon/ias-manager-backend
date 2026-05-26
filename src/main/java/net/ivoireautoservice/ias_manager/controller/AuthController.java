package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.dto.AuthenticationRequest;
import net.ivoireautoservice.ias_manager.auth.dto.AuthenticationResponse;
import net.ivoireautoservice.ias_manager.auth.JwtService;
import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.services.SecurityService;
import net.ivoireautoservice.ias_manager.services.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurService utilisateurService;
    private final SecurityService securityService;

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

    @PutMapping("/password/change")
    public ResponseEntity<UtilisateurDto> changePassword(@RequestBody ChangePasswordRequest request) {
        Utilisateur connected = securityService.getUtilisateurConnecte();
        return ResponseEntity.ok(utilisateurService.changePasswordSelf(connected, request.password()));
    }

    private AuthenticationResponse buildResponse(String token, Utilisateur user) {
        String role = user.getAuthorities().stream()
                .findFirst()
                .map(a -> Objects.requireNonNull(a.getAuthority()).replace("ROLE_", ""))
                .orElse(null);
        Boolean hasChanged = user.getHasChangePassword();
        return new AuthenticationResponse(
                token,
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                role,
                hasChanged != null ? hasChanged : Boolean.TRUE
        );
    }

    public record ChangePasswordRequest(@NotBlank String password) {}
}
