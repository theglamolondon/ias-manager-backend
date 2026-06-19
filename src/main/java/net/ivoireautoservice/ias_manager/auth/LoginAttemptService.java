package net.ivoireautoservice.ias_manager.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation des tentatives de connexion (S7) — protection anti brute-force.
 *
 * <p>Compteur en mémoire par identifiant (email). Au-delà de
 * {@code ias.security.login-max-attempts} échecs, l'identifiant est bloqué
 * pendant {@code ias.security.login-block-minutes}. Une connexion réussie
 * réinitialise le compteur.</p>
 *
 * <p>Implémentation volontairement simple (mono-instance, en mémoire). Pour un
 * déploiement multi-instances, déporter ce compteur vers un store partagé (Redis).</p>
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration blockDuration;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(JwtProperties props) {
        this.maxAttempts = props.getLoginMaxAttempts() > 0 ? props.getLoginMaxAttempts() : 5;
        long minutes = props.getLoginBlockMinutes() > 0 ? props.getLoginBlockMinutes() : 15;
        this.blockDuration = Duration.ofMinutes(minutes);
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(normaliser(key));
        if (attempt == null || attempt.count < maxAttempts) {
            return false;
        }
        if (Instant.now().isAfter(attempt.lastFailure.plus(blockDuration))) {
            // Fenêtre de blocage expirée : on repart à zéro.
            attempts.remove(normaliser(key));
            return false;
        }
        return true;
    }

    public void loginFailed(String key) {
        attempts.compute(normaliser(key), (k, existing) -> {
            int count = existing == null ? 0 : existing.count;
            return new Attempt(count + 1, Instant.now());
        });
    }

    public void loginSucceeded(String key) {
        attempts.remove(normaliser(key));
    }

    private String normaliser(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private record Attempt(int count, Instant lastFailure) {}
}
