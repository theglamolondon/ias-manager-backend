package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityService — récupération de l'utilisateur connecté")
class SecurityServiceTest {

	private final SecurityService service = new SecurityService();

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private static void authentifier(Object principal) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	@Test
	@DisplayName("retourne l'utilisateur présent dans le contexte")
	void getUtilisateurConnecte_present() {
		Utilisateur utilisateur = Utilisateur.builder().id(1L).email("agent@ias.ci").build();
		authentifier(utilisateur);

		assertThat(service.getUtilisateurConnecte()).isSameAs(utilisateur);
		assertThat(service.getUtilisateurConnecteOrNull()).isSameAs(utilisateur);
	}

	@Test
	@DisplayName("lève IllegalStateException sans authentification")
	void getUtilisateurConnecte_sansAuthentification() {
		assertThatThrownBy(service::getUtilisateurConnecte)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Aucun utilisateur connecté");
	}

	@Test
	@DisplayName("lève IllegalStateException si le principal n'est pas un Utilisateur")
	void getUtilisateurConnecte_principalEtranger() {
		authentifier("anonymousUser");

		assertThatThrownBy(service::getUtilisateurConnecte)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("la variante tolérante retourne null au lieu de lever")
	void getUtilisateurConnecteOrNull_absent() {
		assertThat(service.getUtilisateurConnecteOrNull()).isNull();

		authentifier("anonymousUser");
		assertThat(service.getUtilisateurConnecteOrNull()).isNull();
	}
}
