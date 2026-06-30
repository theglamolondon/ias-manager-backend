package net.ivoireautoservice.ias_manager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Sans configuration explicite, Spring Security retombe sur {@code Http403ForbiddenEntryPoint}
 * pour toute requête non authentifiée (token absent/expiré/invalide), ce qui empêche le
 * frontend de distinguer "session expirée" (401, doit rafraîchir ou se reconnecter) de
 * "droits insuffisants" (403, message d'erreur simple). Ce point d'entrée force un 401.
 *
 * <p>Le corps est écrit à la main (pas d'{@code ObjectMapper} injecté) : ce projet tourne sur
 * Spring Boot 4 / Jackson 3 ({@code tools.jackson.databind.ObjectMapper}), pas Jackson 2.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":401,"message":"Authentification requise ou session expirée","timestamp":"%s"}
                """.formatted(LocalDateTime.now()));
    }
}
