package net.ivoireautoservice.ias_manager.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginAttemptService — verrouillage anti brute-force")
class LoginAttemptServiceTest {

	private static LoginAttemptService service(int maxAttempts, long blockMinutes) {
		JwtProperties props = new JwtProperties();
		props.setLoginMaxAttempts(maxAttempts);
		props.setLoginBlockMinutes(blockMinutes);
		return new LoginAttemptService(props);
	}

	@Test
	@DisplayName("un identifiant inconnu n'est pas bloqué")
	void isBlocked_identifiantInconnu() {
		assertThat(service(3, 15).isBlocked("inconnu@ias.ci")).isFalse();
	}

	@Test
	@DisplayName("le blocage ne survient qu'au seuil d'échecs configuré")
	void isBlocked_auSeuil() {
		LoginAttemptService service = service(3, 15);

		service.loginFailed("agent@ias.ci");
		service.loginFailed("agent@ias.ci");
		assertThat(service.isBlocked("agent@ias.ci")).isFalse();

		service.loginFailed("agent@ias.ci");
		assertThat(service.isBlocked("agent@ias.ci")).isTrue();
	}

	@Test
	@DisplayName("une connexion réussie remet le compteur à zéro")
	void loginSucceeded_reinitialise() {
		LoginAttemptService service = service(2, 15);
		service.loginFailed("agent@ias.ci");
		service.loginFailed("agent@ias.ci");
		assertThat(service.isBlocked("agent@ias.ci")).isTrue();

		service.loginSucceeded("agent@ias.ci");

		assertThat(service.isBlocked("agent@ias.ci")).isFalse();
	}

	@Test
	@DisplayName("une durée de blocage non positive retombe sur la valeur par défaut")
	void dureeBlocageParDefaut() {
		JwtProperties props = new JwtProperties();
		props.setLoginMaxAttempts(1);
		props.setLoginBlockMinutes(-1);
		LoginAttemptService parDefaut = new LoginAttemptService(props);

		parDefaut.loginFailed("agent@ias.ci");

		// 15 minutes par défaut : le blocage tient toujours immédiatement après l'échec.
		assertThat(parDefaut.isBlocked("agent@ias.ci")).isTrue();
	}

	@Test
	@DisplayName("l'identifiant est normalisé (casse et espaces)")
	void normalisation_casseEtEspaces() {
		LoginAttemptService service = service(2, 15);

		service.loginFailed("Agent@IAS.ci");
		service.loginFailed("  agent@ias.ci  ");

		assertThat(service.isBlocked("AGENT@IAS.CI")).isTrue();
	}

	@Test
	@DisplayName("un identifiant null est toléré et traité comme la chaîne vide")
	void identifiantNull() {
		LoginAttemptService service = service(1, 15);

		service.loginFailed(null);

		assertThat(service.isBlocked(null)).isTrue();
		assertThat(service.isBlocked("")).isTrue();
	}

	@Test
	@DisplayName("le blocage d'un identifiant n'affecte pas les autres")
	void isBlocked_isolationParIdentifiant() {
		LoginAttemptService service = service(1, 15);

		service.loginFailed("agent@ias.ci");

		assertThat(service.isBlocked("agent@ias.ci")).isTrue();
		assertThat(service.isBlocked("autre@ias.ci")).isFalse();
	}

	@Test
	@DisplayName("le seuil retombe sur 5 quand la configuration est absente")
	void seuilParDefaut() {
		LoginAttemptService service = service(0, 15);

		for (int i = 0; i < 4; i++) {
			service.loginFailed("agent@ias.ci");
		}
		assertThat(service.isBlocked("agent@ias.ci")).isFalse();

		service.loginFailed("agent@ias.ci");
		assertThat(service.isBlocked("agent@ias.ci")).isTrue();
	}
}
