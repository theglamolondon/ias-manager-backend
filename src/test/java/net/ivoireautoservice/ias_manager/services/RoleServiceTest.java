package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.Role;
import net.ivoireautoservice.ias_manager.dto.request.RoleRequest;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.RoleMapper;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService — gestion des rôles et protection des rôles système")
class RoleServiceTest {

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private RoleMapper roleMapper;

	@InjectMocks
	private RoleService service;

	private static RoleEntity role(Long id, String nom, boolean systeme, PermissionEnum... permissions) {
		return RoleEntity.builder()
				.id(id).nom(nom).systemRole(systeme)
				.permissions(new HashSet<>(Set.of(permissions)))
				.build();
	}

	@Nested
	@DisplayName("Création")
	class Creation {

		@Test
		@DisplayName("le nom est normalisé (espaces retirés) et le rôle n'est jamais système")
		void create_normaliseEtNonSysteme() {
			RoleRequest request = RoleRequest.builder()
					.nom("  SUPERVISEUR  ")
					.permissions(Set.of(PermissionEnum.VEHICULE_READ))
					.build();
			RoleEntity entity = new RoleEntity();
			when(roleRepository.existsByNom("SUPERVISEUR")).thenReturn(false);
			when(roleMapper.toEntity(request)).thenReturn(entity);
			when(roleRepository.save(entity)).thenReturn(entity);
			when(roleMapper.toDto(entity)).thenReturn(Role.builder().build());

			service.createRole(request);

			assertThat(entity.getNom()).isEqualTo("SUPERVISEUR");
			assertThat(entity.getSystemRole()).isFalse();
			assertThat(entity.getPermissions()).containsExactly(PermissionEnum.VEHICULE_READ);
		}

		@Test
		@DisplayName("refuse un nom déjà pris")
		void create_nomDuplique() {
			when(roleRepository.existsByNom("ADMIN")).thenReturn(true);

			assertThatThrownBy(() -> service.createRole(RoleRequest.builder().nom("ADMIN").build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("existe déjà");
			verify(roleRepository, never()).save(any());
		}

		@Test
		@DisplayName("une liste de permissions nulle donne un rôle sans permission")
		void create_permissionsNulles() {
			RoleRequest request = RoleRequest.builder().nom("VIDE").permissions(null).build();
			RoleEntity entity = new RoleEntity();
			when(roleRepository.existsByNom("VIDE")).thenReturn(false);
			when(roleMapper.toEntity(request)).thenReturn(entity);
			when(roleRepository.save(entity)).thenReturn(entity);
			when(roleMapper.toDto(entity)).thenReturn(Role.builder().build());

			service.createRole(request);

			assertThat(entity.getPermissions()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Modification")
	class Modification {

		@Test
		@DisplayName("un rôle métier voit son nom et ses permissions mis à jour")
		void update_roleMetier() {
			RoleEntity entity = role(5L, "SUPERVISEUR", false, PermissionEnum.VEHICULE_READ);
			RoleRequest request = RoleRequest.builder()
					.nom("SUPERVISEUR FLOTTE")
					.permissions(Set.of(PermissionEnum.MISSION_READ))
					.build();
			when(roleRepository.findById(5L)).thenReturn(Optional.of(entity));
			when(roleRepository.existsByNom("SUPERVISEUR FLOTTE")).thenReturn(false);
			when(roleRepository.save(entity)).thenReturn(entity);
			when(roleMapper.toDto(entity)).thenReturn(Role.builder().build());

			service.updateRole(5L, request);

			assertThat(entity.getNom()).isEqualTo("SUPERVISEUR FLOTTE");
			assertThat(entity.getPermissions()).containsExactly(PermissionEnum.MISSION_READ);
		}

		@Test
		@DisplayName("refuse de renommer un rôle système")
		void update_renommageRoleSysteme() {
			RoleEntity admin = role(1L, "ADMIN", true, PermissionEnum.VEHICULE_READ);
			when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));

			assertThatThrownBy(() -> service.updateRole(1L,
					RoleRequest.builder().nom("SUPER_ADMIN").build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("nom d'un rôle système");
		}

		@Test
		@DisplayName("refuse de modifier les permissions d'un rôle système (anti-escalade)")
		void update_permissionsRoleSysteme() {
			RoleEntity admin = role(1L, "ADMIN", true, PermissionEnum.VEHICULE_READ);
			when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));

			assertThatThrownBy(() -> service.updateRole(1L, RoleRequest.builder()
					.nom("ADMIN")
					.permissions(Set.of(PermissionEnum.VEHICULE_READ, PermissionEnum.ROLE_MANAGE))
					.build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("permissions d'un rôle système");
		}

		@Test
		@DisplayName("un rôle système reste modifiable sur son libellé si les permissions sont inchangées")
		void update_roleSystemeLibelleSeul() {
			RoleEntity admin = role(1L, "ADMIN", true, PermissionEnum.VEHICULE_READ);
			RoleRequest request = RoleRequest.builder()
					.nom("ADMIN").libelle("Administrateur général")
					.permissions(Set.of(PermissionEnum.VEHICULE_READ))
					.build();
			when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));
			when(roleRepository.save(admin)).thenReturn(admin);
			when(roleMapper.toDto(admin)).thenReturn(Role.builder().build());

			service.updateRole(1L, request);

			verify(roleMapper).updateEntity(request, admin);
			assertThat(admin.getPermissions()).containsExactly(PermissionEnum.VEHICULE_READ);
		}

		@Test
		@DisplayName("refuse un renommage vers un nom déjà pris")
		void update_nomDuplique() {
			RoleEntity entity = role(5L, "SUPERVISEUR", false);
			when(roleRepository.findById(5L)).thenReturn(Optional.of(entity));
			when(roleRepository.existsByNom("ACHAT")).thenReturn(true);

			assertThatThrownBy(() -> service.updateRole(5L, RoleRequest.builder().nom("ACHAT").build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("existe déjà");
		}

		@Test
		@DisplayName("lève 404 sur un id inconnu")
		void update_absent() {
			when(roleRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.updateRole(99L, RoleRequest.builder().nom("X").build()))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Suppression")
	class Suppression {

		@Test
		@DisplayName("refuse de supprimer un rôle système")
		void delete_roleSysteme() {
			RoleEntity admin = role(1L, "ADMIN", true);
			when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));

			assertThatThrownBy(() -> service.deleteRole(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("rôle système");
			verify(roleRepository, never()).delete(any());
		}

		@Test
		@DisplayName("supprime un rôle métier")
		void delete_roleMetier() {
			RoleEntity entity = role(5L, "SUPERVISEUR", false);
			when(roleRepository.findById(5L)).thenReturn(Optional.of(entity));

			service.deleteRole(5L);

			verify(roleRepository).delete(entity);
		}

		@Test
		@DisplayName("lève 404 sur un id inconnu")
		void delete_absent() {
			when(roleRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.deleteRole(99L))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}
}
