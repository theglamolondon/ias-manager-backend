package net.ivoireautoservice.ias_manager.auth.dto;

public record AuthenticationResponse(
        String token,
        String nom,
        String prenoms,
        String email,
        String role
) {}
