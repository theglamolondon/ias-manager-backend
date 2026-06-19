package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.LoginAttemptService;
import net.ivoireautoservice.ias_manager.auth.dto.AuthenticationRequest;
import net.ivoireautoservice.ias_manager.auth.dto.AuthenticationResponse;
import net.ivoireautoservice.ias_manager.auth.JwtService;
import net.ivoireautoservice.ias_manager.auth.UtilisateurDetailsService;
import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.services.SecurityService;
import net.ivoireautoservice.ias_manager.services.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurService utilisateurService;
    private final SecurityService securityService;
    private final LoginAttemptService loginAttemptService;
    private final UtilisateurDetailsService utilisateurDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        final String email = request.email();
        if (loginAttemptService.isBlocked(email)) {
            throw new BadRequestException(
                    "Trop de tentatives de connexion. Réessayez dans quelques minutes.");
        }

        final Utilisateur user;
        try {
            var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
            );
            user = (Utilisateur) auth.getPrincipal();
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(email);
            throw ex;
        }

        loginAttemptService.loginSucceeded(email);
        final String jwt = jwtService.generateToken(user);
        final String refresh = jwtService.generateRefreshToken(user);
        return ResponseEntity.ok(buildResponse(jwt, refresh, user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody RefreshRequest request) {
        final String refreshToken = request.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token manquant");
        }
        final String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception ex) {
            throw new BadRequestException("Refresh token invalide");
        }
        Utilisateur user = utilisateurDetailsService.loadUserByUsername(email);
        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new BadRequestException("Refresh token invalide ou expiré");
        }
        final String jwt = jwtService.generateToken(user);
        final String newRefresh = jwtService.generateRefreshToken(user);
        return ResponseEntity.ok(buildResponse(jwt, newRefresh, user));
    }

    @GetMapping("/info")
    public ResponseEntity<AuthenticationResponse> info(@AuthenticationPrincipal Utilisateur user) {
        return ResponseEntity.ok(buildResponse(null, null, user));
    }

    @PutMapping("/password/change")
    public ResponseEntity<UtilisateurDto> changePassword(@RequestBody ChangePasswordRequest request) {
        Utilisateur connected = securityService.getUtilisateurConnecte();
        return ResponseEntity.ok(utilisateurService.changePasswordSelf(connected, request.password()));
    }

    private AuthenticationResponse buildResponse(String token, String refreshToken, Utilisateur user) {
        Boolean hasChanged = user.getHasChangePassword();
        return new AuthenticationResponse(
                token,
                refreshToken,
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getPrimaryRoleName(),
                hasChanged != null ? hasChanged : Boolean.TRUE,
                user.getRoleNames(),
                user.getGroupeNames(),
                user.getPermissionNames()
        );
    }

    public record ChangePasswordRequest(@NotBlank String password) {}

    public record RefreshRequest(String refreshToken) {}
}
