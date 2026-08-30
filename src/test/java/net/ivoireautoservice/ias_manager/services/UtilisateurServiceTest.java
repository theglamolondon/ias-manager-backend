package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.dto.request.UtilisateurRequest;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.UtilisateurMapper;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import net.ivoireautoservice.ias_manager.repository.GroupeRepository;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UtilisateurService — comptes, mots de passe et attribution RBAC")
class UtilisateurServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmployeRepository employeRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private GroupeRepository groupeRepository;

	@Mock
	private UtilisateurMapper utilisateurMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UtilisateurService service;

	private static UtilisateurRequest request(String password) {
		return UtilisateurRequest.builder()
				.nom("Kouassi").prenom("Yao").email("agent@ias.ci").password(password).build();
	}

	@Nested
	@DisplayName("Politique de mot de passe")
	class PolitiqueMotDePasse {

		@ParameterizedTest
		@ValueSource(strings = {"court1", "a1b2c3d", ""})
		@DisplayName("refuse un mot de passe de moins de 8 caractères")
		void tropCourt(String password) {
			assertThatThrownBy(() -> service.createUtilisateur(request(password)))
					.isInstanceOf(BadRequestException.class);
			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("refuse un mot de passe sans chiffre")
		void sansChiffre() {
			assertThatThrownBy(() -> service.createUtilisateur(request("motdepassesansdigit")))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("une lettre et un chiffre");
		}

		@Test
		@DisplayName("refuse un mot de passe sans lettre")
		void sansLettre() {
			assertThatThrownBy(() -> service.createUtilisateur(request("1234567890")))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("une lettre et un chiffre");
		}

		@Test
		@DisplayName("refuse un mot de passe absent à la création")
		void absentALaCreation() {
			assertThatThrownBy(() -> service.createUtilisateur(request(null)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("obligatoire à la création");
		}

		@Test
		@DisplayName("accepte 8 caractères mêlant lettres et chiffres")
		void valide() {
			UtilisateurRequest req = request("Passw0rd");
			Utilisateur entity = new Utilisateur();
			when(utilisateurMapper.toEntity(req)).thenReturn(entity);
			when(passwordEncoder.encode("Passw0rd")).thenReturn("$hash");
			when(userRepository.save(entity)).thenReturn(entity);
			when(utilisateurMapper.toDto(entity)).thenReturn(new UtilisateurDto());

			service.createUtilisateur(req);

			assertThat(entity.getPassword()).isEqualTo("$hash");
		}
	}

	@Nested
	@DisplayName("Création et mise à jour")
	class CreationMiseAJour {

		@Test
		@DisplayName("un nouvel utilisateur doit changer son mot de passe à la première connexion")
		void creation_forceChangementMotDePasse() {
			UtilisateurRequest req = request("Passw0rd");
			Utilisateur entity = new Utilisateur();
			when(utilisateurMapper.toEntity(req)).thenReturn(entity);
			when(passwordEncoder.encode(anyString())).thenReturn("$hash");
			when(userRepository.save(entity)).thenReturn(entity);
			when(utilisateurMapper.toDto(entity)).thenReturn(new UtilisateurDto());

			service.createUtilisateur(req);

			assertThat(entity.getHasChangePassword()).isFalse();
		}

		@Test
		@DisplayName("le mot de passe est toujours haché, jamais stocké en clair")
		void creation_hachage() {
			UtilisateurRequest req = request("Passw0rd");
			Utilisateur entity = new Utilisateur();
			when(utilisateurMapper.toEntity(req)).thenReturn(entity);
			when(passwordEncoder.encode("Passw0rd")).thenReturn("$2a$hash");
			when(userRepository.save(entity)).thenReturn(entity);
			when(utilisateurMapper.toDto(entity)).thenReturn(new UtilisateurDto());

			service.createUtilisateur(req);

			assertThat(entity.getPassword()).isNotEqualTo("Passw0rd");
			verify(passwordEncoder).encode("Passw0rd");
		}

		@Test
		@DisplayName("un employeId inconnu lève 404")
		void creation_employeInconnu() {
			UtilisateurRequest req = UtilisateurRequest.builder()
					.email("a@ias.ci").password("Passw0rd").employeId(99L).build();
			when(utilisateurMapper.toEntity(req)).thenReturn(new Utilisateur());
			when(passwordEncoder.encode(anyString())).thenReturn("$hash");
			when(employeRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createUtilisateur(req))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Employé avec l'id 99");
		}

		@Test
		@DisplayName("l'employé est rattaché quand employeId est fourni")
		void creation_employeRattache() {
			EmployeEntity employe = EmployeEntity.builder().id(7L).build();
			UtilisateurRequest req = UtilisateurRequest.builder()
					.email("a@ias.ci").password("Passw0rd").employeId(7L).build();
			Utilisateur entity = new Utilisateur();
			when(utilisateurMapper.toEntity(req)).thenReturn(entity);
			when(passwordEncoder.encode(anyString())).thenReturn("$hash");
			when(employeRepository.findById(7L)).thenReturn(Optional.of(employe));
			when(userRepository.save(entity)).thenReturn(entity);
			when(utilisateurMapper.toDto(entity)).thenReturn(new UtilisateurDto());

			service.createUtilisateur(req);

			assertThat(entity.getEmploye()).isSameAs(employe);
		}

		@Test
		@DisplayName("une mise à jour sans mot de passe conserve le hash et le flag existants")
		void update_sansMotDePasse() {
			Utilisateur entity = Utilisateur.builder()
					.id(1L).password("$ancien").hasChangePassword(true).build();
			UtilisateurRequest req = UtilisateurRequest.builder().email("a@ias.ci").build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(userRepository.save(entity)).thenReturn(entity);
			when(utilisateurMapper.toDto(entity)).thenReturn(new UtilisateurDto());

			service.updateUtilisateur(1L, req);

			assertThat(entity.getPassword()).isEqualTo("$ancien");
			assertThat(entity.getHasChangePassword()).isTrue();
			verify(passwordEncoder, never()).encode(anyString());
		}

		@Test
		@DisplayName("une réinitialisation par un admin repositionne le flag de premier changement")
		void update_reinitialisationAdmin() {
			Utilisateur entity = Utilisateur.builder()
					.id(1L).password("$ancien").hasChangePassword(true).build();
			UtilisateurRequest req = UtilisateurRequest.builder()
					.email("a@ias.ci").password("Nouveau12").build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(passwordEncoder.encode("Nouveau12")).thenReturn("$nouveau");
			when(userRepository.save(entity)).thenReturn(entity);
			when(utilisateurMapper.toDto(entity)).thenReturn(new UtilisateurDto());

			service.updateUtilisateur(1L, req);

			assertThat(entity.getPassword()).isEqualTo("$nouveau");
			assertThat(entity.getHasChangePassword()).isFalse();
		}

		@Test
		@DisplayName("la politique de mot de passe s'applique aussi à la mise à jour")
		void update_politiqueAppliquee() {
			Utilisateur entity = Utilisateur.builder().id(1L).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.updateUtilisateur(1L, UtilisateurRequest.builder()
					.email("a@ias.ci").password("court").build()))
					.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("supprimer un utilisateur inconnu lève 404")
		void delete_absent() {
			when(userRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteUtilisateur(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(userRepository, never()).deleteById(any());
		}
	}

	@Nested
	@DisplayName("Changement de mot de passe self-service")
	class ChangementSelfService {

		@Test
		@DisplayName("lève le flag hasChangePassword")
		void leveLeFlag() {
			Utilisateur connecte = Utilisateur.builder().id(1L).hasChangePassword(false).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(connecte));
			when(passwordEncoder.encode("Nouveau12")).thenReturn("$nouveau");
			when(userRepository.save(connecte)).thenReturn(connecte);
			when(utilisateurMapper.toDto(connecte)).thenReturn(new UtilisateurDto());

			service.changePasswordSelf(connecte, "Nouveau12");

			assertThat(connecte.getHasChangePassword()).isTrue();
			assertThat(connecte.getPassword()).isEqualTo("$nouveau");
		}

		@Test
		@DisplayName("refuse un mot de passe vide")
		void refuseVide() {
			Utilisateur connecte = Utilisateur.builder().id(1L).build();

			assertThatThrownBy(() -> service.changePasswordSelf(connecte, "   "))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("obligatoire");
		}

		@Test
		@DisplayName("refuse un mot de passe ne respectant pas la politique")
		void refusePolitique() {
			Utilisateur connecte = Utilisateur.builder().id(1L).build();

			assertThatThrownBy(() -> service.changePasswordSelf(connecte, "sansdechiffre"))
					.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("lève 404 si le compte connecté a disparu de la base")
		void compteDisparu() {
			Utilisateur connecte = Utilisateur.builder().id(1L).build();
			when(userRepository.findById(1L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.changePasswordSelf(connecte, "Nouveau12"))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Attribution des rôles et groupes")
	class AttributionRbac {

		@Test
		@DisplayName("assignRoles remplace intégralement l'ensemble des rôles directs")
		void assignRoles() {
			RoleEntity ancien = RoleEntity.builder().id(1L).nom("ANCIEN").build();
			RoleEntity nouveau = RoleEntity.builder().id(2L).nom("NOUVEAU").build();
			Utilisateur user = Utilisateur.builder().id(1L)
					.roles(new java.util.HashSet<>(Set.of(ancien))).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(user));
			when(roleRepository.findById(2L)).thenReturn(Optional.of(nouveau));
			when(userRepository.save(user)).thenReturn(user);
			when(utilisateurMapper.toDto(user)).thenReturn(new UtilisateurDto());

			service.assignRoles(1L, Set.of(2L));

			assertThat(user.getRoles()).containsExactly(nouveau);
		}

		@Test
		@DisplayName("assignRoles avec null retire tous les rôles")
		void assignRoles_null() {
			Utilisateur user = Utilisateur.builder().id(1L)
					.roles(new java.util.HashSet<>(Set.of(RoleEntity.builder().id(1L).build()))).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(user));
			when(userRepository.save(user)).thenReturn(user);
			when(utilisateurMapper.toDto(user)).thenReturn(new UtilisateurDto());

			service.assignRoles(1L, null);

			assertThat(user.getRoles()).isEmpty();
		}

		@Test
		@DisplayName("assignRoles échoue si un rôle référencé n'existe pas")
		void assignRoles_roleInconnu() {
			Utilisateur user = Utilisateur.builder().id(1L).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(user));
			when(roleRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.assignRoles(1L, Set.of(99L)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Rôle avec l'id 99");
			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("assignGroupes remplace intégralement l'appartenance aux groupes")
		void assignGroupes() {
			GroupeEntity groupe = GroupeEntity.builder().id(10L).nom("FLOTTE").build();
			Utilisateur user = Utilisateur.builder().id(1L).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(user));
			when(groupeRepository.findById(10L)).thenReturn(Optional.of(groupe));
			when(userRepository.save(user)).thenReturn(user);
			when(utilisateurMapper.toDto(user)).thenReturn(new UtilisateurDto());

			service.assignGroupes(1L, Set.of(10L));

			assertThat(user.getGroupes()).containsExactly(groupe);
		}

		@Test
		@DisplayName("assignGroupes échoue si un groupe référencé n'existe pas")
		void assignGroupes_groupeInconnu() {
			Utilisateur user = Utilisateur.builder().id(1L).build();
			when(userRepository.findById(1L)).thenReturn(Optional.of(user));
			when(groupeRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.assignGroupes(1L, Set.of(99L)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Groupe avec l'id 99");
		}

		@Test
		@DisplayName("assigner sur un utilisateur inconnu lève 404")
		void assign_utilisateurInconnu() {
			when(userRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.assignRoles(99L, Set.of()))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}
}
