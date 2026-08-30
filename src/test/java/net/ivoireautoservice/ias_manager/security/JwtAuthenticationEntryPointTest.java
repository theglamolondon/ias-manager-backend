package net.ivoireautoservice.ias_manager.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtAuthenticationEntryPoint — réponse 401 sur requête non authentifiée")
class JwtAuthenticationEntryPointTest {

	private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

	@Test
	@DisplayName("répond 401 (et non 403) en JSON UTF-8")
	void commence_repond401Json() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new InsufficientAuthenticationException("pas de token"));

		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
	}

	@Test
	@DisplayName("le corps expose le statut et un message de session expirée")
	void commence_corpsExplicite() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(new MockHttpServletRequest(), response,
				new InsufficientAuthenticationException("pas de token"));

		String corps = response.getContentAsString();
		assertThat(corps).contains("\"status\":401");
		assertThat(corps).contains("session expirée");
		assertThat(corps).contains("\"timestamp\"");
	}
}
