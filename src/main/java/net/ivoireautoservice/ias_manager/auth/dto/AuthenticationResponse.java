package net.ivoireautoservice.ias_manager.auth.dto;

import java.util.Set;

public record AuthenticationResponse(
        String token,
        String refreshToken,
        String nom,
        String prenoms,
        String email,
        String role,
        Boolean hasChangePassword,
        Set<String> roles,
        Set<String> groupes,
        Set<String> permissions
) {}
