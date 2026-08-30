package net.ivoireautoservice.ias_manager.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService — émission et validation des jetons")
class JwtServiceTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(
			"secret-de-test-ias-manager-suffisamment-long-pour-hmac-sha256".getBytes(StandardCharsets.UTF_8));
	private static final String AUTRE_SECRET = Base64.getEncoder().encodeToString(
			"un-tout-autre-secret-de-test-egalement-assez-long-pour-hs256".getBytes(StandardCharsets.UTF_8));

	private JwtService jwtService;
	private UserDetails utilisateur;

	private static JwtProperties props(String secret, long expiration, long refreshExpiration) {
		JwtProperties props = new JwtProperties();
		props.setSecret(secret);
		props.setExpiration(expiration);
		props.setRefreshExpiration(refreshExpiration);
		props.setIssuer("ias-manager-api-test");
		return props;
	}

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(props(SECRET, 60_000L, 600_000L));
		utilisateur = new User("agent@ias.ci", "motdepasse",
				List.of(new SimpleGrantedAuthority("VEHICULE_READ")));
	}

	@Test
	@DisplayName("le token d'accès porte l'email en sujet")
	void generateToken_sujetEstEmail() {
		String token = jwtService.generateToken(utilisateur);

		assertThat(jwtService.extractUsername(token)).isEqualTo("agent@ias.ci");
	}

	@Test
	@DisplayName("un token d'accès fraîchement émis est valide pour son porteur")
	void isTokenValid_tokenFrais() {
		String token = jwtService.generateToken(utilisateur);

		assertThat(jwtService.isTokenValid(token, utilisateur)).isTrue();
	}

	@Test
	@DisplayName("un token émis pour un autre utilisateur est rejeté")
	void isTokenValid_autreUtilisateur() {
		String token = jwtService.generateToken(utilisateur);
		UserDetails autre = new User("autre@ias.ci", "x", List.of());

		assertThat(jwtService.isTokenValid(token, autre)).isFalse();
	}

	@Test
	@DisplayName("un token expiré lève ExpiredJwtException à la lecture")
	void extractUsername_tokenExpire() {
		JwtService expire = new JwtService(props(SECRET, -1_000L, -1_000L));
		String token = expire.generateToken(utilisateur);

		assertThatThrownBy(() -> expire.isTokenValid(token, utilisateur))
				.isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	@DisplayName("un token signé avec une autre clé est rejeté")
	void extractUsername_signatureInvalide() {
		String token = new JwtService(props(AUTRE_SECRET, 60_000L, 600_000L)).generateToken(utilisateur);

		assertThatThrownBy(() -> jwtService.extractUsername(token))
				.isInstanceOf(SignatureException.class);
	}

	@Test
	@DisplayName("un refresh token est reconnu comme tel")
	void isRefreshTokenValid_refreshToken() {
		String refresh = jwtService.generateRefreshToken(utilisateur);

		assertThat(jwtService.isRefreshTokenValid(refresh, utilisateur)).isTrue();
	}

	@Test
	@DisplayName("un token d'accès n'est pas accepté comme refresh token")
	void isRefreshTokenValid_refuseTokenAcces() {
		String acces = jwtService.generateToken(utilisateur);

		assertThat(jwtService.isRefreshTokenValid(acces, utilisateur)).isFalse();
	}

	@Test
	@DisplayName("un refresh token émis pour un autre utilisateur est rejeté")
	void isRefreshTokenValid_autreUtilisateur() {
		String refresh = jwtService.generateRefreshToken(utilisateur);
		UserDetails autre = new User("autre@ias.ci", "x", List.of());

		assertThat(jwtService.isRefreshTokenValid(refresh, autre)).isFalse();
	}

	@Test
	@DisplayName("le refresh token vit plus longtemps que le token d'accès")
	void refreshToken_dureeDeVieSuperieure() {
		String acces = jwtService.generateToken(utilisateur);
		String refresh = jwtService.generateRefreshToken(utilisateur);

		JwtService courtAcces = new JwtService(props(SECRET, -1_000L, 600_000L));
		String accesExpire = courtAcces.generateToken(utilisateur);

		assertThat(acces).isNotEqualTo(refresh);
		assertThatThrownBy(() -> jwtService.extractUsername(accesExpire))
				.isInstanceOf(ExpiredJwtException.class);
		assertThat(jwtService.isRefreshTokenValid(refresh, utilisateur)).isTrue();
	}
}
