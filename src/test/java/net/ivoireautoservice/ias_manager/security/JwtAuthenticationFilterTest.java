package net.ivoireautoservice.ias_manager.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import net.ivoireautoservice.ias_manager.auth.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter — authentification par en-tête Bearer")
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtAuthenticationFilter filter;

	private final MockHttpServletRequest request = new MockHttpServletRequest();
	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final UserDetails utilisateur = new User("agent@ias.ci", "x",
			List.of(new SimpleGrantedAuthority("VEHICULE_READ")));

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("un token valide place l'authentification dans le contexte")
	void tokenValide_authentifie() throws Exception {
		request.addHeader("Authorization", "Bearer un-token");
		when(jwtService.extractUsername("un-token")).thenReturn("agent@ias.ci");
		when(userDetailsService.loadUserByUsername("agent@ias.ci")).thenReturn(utilisateur);
		when(jwtService.isTokenValid("un-token", utilisateur)).thenReturn(true);

		filter.doFilter(request, response, filterChain);

		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isSameAs(utilisateur);
		assertThat(auth.getAuthorities()).extracting("authority").containsExactly("VEHICULE_READ");
		assertThat(auth.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("sans en-tête Authorization la requête passe en anonyme")
	void sansEnTete_anonyme() throws Exception {
		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService, userDetailsService);
	}

	@Test
	@DisplayName("un en-tête sans préfixe Bearer est ignoré")
	void enTeteNonBearer_ignore() throws Exception {
		request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService, userDetailsService);
	}

	@Test
	@DisplayName("un token expiré ne fait pas échouer le filtre : la chaîne continue en anonyme")
	void tokenExpire_poursuitEnAnonyme() throws Exception {
		request.addHeader("Authorization", "Bearer token-expire");
		when(jwtService.extractUsername("token-expire"))
				.thenThrow(new ExpiredJwtException(null, null, "expiré"));

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain, times(1)).doFilter(request, response);
		verifyNoInteractions(userDetailsService);
	}

	@Test
	@DisplayName("un token malformé ne fait pas échouer le filtre")
	void tokenMalforme_poursuitEnAnonyme() throws Exception {
		request.addHeader("Authorization", "Bearer n-importe-quoi");
		when(jwtService.extractUsername(anyString())).thenThrow(new MalformedJwtException("malformé"));

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("un token dont la validation échoue ne crée pas d'authentification")
	void tokenInvalide_pasDAuthentification() throws Exception {
		request.addHeader("Authorization", "Bearer token-invalide");
		when(jwtService.extractUsername("token-invalide")).thenReturn("agent@ias.ci");
		when(userDetailsService.loadUserByUsername("agent@ias.ci")).thenReturn(utilisateur);
		when(jwtService.isTokenValid("token-invalide", utilisateur)).thenReturn(false);

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("une authentification déjà présente n'est pas réécrite")
	void contexteDejaAuthentifie_nonEcrase() throws Exception {
		var existante = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				"deja-la", null, List.of());
		SecurityContextHolder.getContext().setAuthentication(existante);
		request.addHeader("Authorization", "Bearer un-token");
		when(jwtService.extractUsername("un-token")).thenReturn("agent@ias.ci");

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existante);
		verify(userDetailsService, never()).loadUserByUsername(any());
		verify(filterChain).doFilter(request, response);
	}
}
