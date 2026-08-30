package net.ivoireautoservice.ias_manager.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler — traduction des exceptions en réponses HTTP")
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@SuppressWarnings("unused")
	private void methodeCible(String argument) {
	}

	@Test
	@DisplayName("ResourceNotFoundException → 404 avec le message d'origine")
	void resourceNotFound() {
		var reponse = handler.handleResourceNotFoundException(
				new ResourceNotFoundException("Véhicule", 42L));

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(reponse.getBody()).isNotNull();
		assertThat(reponse.getBody().status()).isEqualTo(404);
		assertThat(reponse.getBody().message()).isEqualTo("Véhicule avec l'id 42 non trouvé");
		assertThat(reponse.getBody().timestamp()).isNotNull();
	}

	@Test
	@DisplayName("BadRequestException → 400 avec le message métier")
	void badRequest() {
		var reponse = handler.handleBadRequestException(new BadRequestException("Solde insuffisant"));

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(reponse.getBody().message()).isEqualTo("Solde insuffisant");
	}

	@Test
	@DisplayName("MaxMediaExceededException → 400")
	void maxMedia() {
		var reponse = handler.handleMaxMediaExceededException(
				new MaxMediaExceededException("Type de fichier non autorisé"));

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(reponse.getBody().message()).isEqualTo("Type de fichier non autorisé");
	}

	@Test
	@DisplayName("BadCredentialsException → 403 avec un message générique (pas de fuite d'information)")
	void badCredentials() {
		var reponse = handler.handleBadCredentialsException(
				new BadCredentialsException("Mot de passe erroné pour agent@ias.ci"));

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(reponse.getBody().message()).isEqualTo("Identifiant ou mot de passe incorrect");
		assertThat(reponse.getBody().message()).doesNotContain("agent@ias.ci");
	}

	@Test
	@DisplayName("AccessDeniedException → 403 avec la permission requise")
	void accessDenied() {
		var reponse = handler.handleAccessDeniedException(
				new AccessDeniedException("Accès refusé : permission 'FACTURE_CLIENT_READ' requise"));

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(reponse.getBody().message()).contains("FACTURE_CLIENT_READ");
	}

	@Test
	@DisplayName("une exception inattendue → 500 sans détail technique")
	void erreurGenerique() {
		var reponse = handler.handleGenericException(
				new IllegalStateException("NullPointerException at line 42 in FactureService"));

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(reponse.getBody().message()).isEqualTo("Une erreur interne s'est produite");
		assertThat(reponse.getBody().message()).doesNotContain("FactureService");
	}

	@Test
	@DisplayName("erreurs de validation → 400 avec la carte champ → message")
	void validation() throws Exception {
		Method methode = GlobalExceptionHandlerTest.class.getDeclaredMethod("methodeCible", String.class);
		BindingResult bindingResult = new BeanPropertyBindingResult(new FauxFormulaire(), "missionRequest");
		bindingResult.rejectValue("tarif", "NotNull", "Le tarif est obligatoire");

		var exception = new MethodArgumentNotValidException(
				new MethodParameter(methode, 0), bindingResult);

		var reponse = handler.handleValidationException(exception);

		assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(reponse.getBody().message()).isEqualTo("Erreur de validation");
		assertThat(reponse.getBody().errors()).containsEntry("tarif", "Le tarif est obligatoire");
	}

	static class FauxFormulaire {
		private String tarif;

		public String getTarif() {
			return tarif;
		}

		public void setTarif(String tarif) {
			this.tarif = tarif;
		}
	}
}
