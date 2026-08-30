package net.ivoireautoservice.ias_manager.auth;

import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UtilisateurDetailsService — chargement par email")
class UtilisateurDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UtilisateurDetailsService service;

	@Test
	@DisplayName("retourne l'utilisateur correspondant à l'email")
	void loadUserByUsername_trouve() {
		Utilisateur utilisateur = Utilisateur.builder().id(1L).email("agent@ias.ci").build();
		when(userRepository.findByEmail("agent@ias.ci")).thenReturn(Optional.of(utilisateur));

		assertThat(service.loadUserByUsername("agent@ias.ci")).isSameAs(utilisateur);
	}

	@Test
	@DisplayName("lève UsernameNotFoundException quand l'email est inconnu")
	void loadUserByUsername_inconnu() {
		when(userRepository.findByEmail("inconnu@ias.ci")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("inconnu@ias.ci"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("inconnu@ias.ci");
	}
}
