package net.ivoireautoservice.ias_manager.config;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.auth.RoleEnum;
import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RbacSeeder — amorçage idempotent du modèle RBAC")
class RbacSeederTest {

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private RbacSeeder seeder;

	@BeforeEach
	void setUp() {
		when(roleRepository.findByNom(anyString())).thenReturn(Optional.empty());
		when(roleRepository.save(any(RoleEntity.class))).thenAnswer(i -> i.getArgument(0));
		when(userRepository.findAll()).thenReturn(List.of());
	}

	@Test
	@DisplayName("ADMIN reçoit l'intégralité du catalogue de permissions")
	void adminRecoitToutesLesPermissions() {
		seeder.run(null);

		ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
		verify(roleRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

		RoleEntity admin = captor.getAllValues().stream()
				.filter(r -> RoleEnum.ADMIN.name().equals(r.getNom()))
				.findFirst().orElseThrow();

		assertThat(admin.getPermissions()).containsExactlyInAnyOrderElementsOf(
				EnumSet.allOf(PermissionEnum.class));
		assertThat(admin.getSystemRole()).isTrue();
	}

	@Test
	@DisplayName("tous les rôles système sont créés au premier démarrage")
	void tousLesRolesSystemeSontCrees() {
		seeder.run(null);

		ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
		verify(roleRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

		assertThat(captor.getAllValues()).extracting(RoleEntity::getNom)
				.contains("ADMIN", "LOGISTIQUE", "COMMERCIAL", "RECOUVREMENT", "RH", "ACHAT");
		assertThat(captor.getAllValues()).allMatch(RoleEntity::getSystemRole);
	}

	@Test
	@DisplayName("un rôle système existant (hors ADMIN) n'est pas réécrit")
	void roleExistantNonEcrase() {
		RoleEntity logistiqueExistant = RoleEntity.builder()
				.id(2L).nom(RoleEnum.LOGISTIQUE.name()).systemRole(true)
				.permissions(new HashSet<>(Set.of(PermissionEnum.DASHBOARD_READ)))
				.build();
		when(roleRepository.findByNom(RoleEnum.LOGISTIQUE.name())).thenReturn(Optional.of(logistiqueExistant));

		seeder.run(null);

		ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
		verify(roleRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
		assertThat(captor.getAllValues()).noneMatch(r -> r == logistiqueExistant);
	}

	@Test
	@DisplayName("ADMIN existant est resynchronisé sur le catalogue complet")
	void adminExistantResynchronise() {
		RoleEntity adminExistant = RoleEntity.builder()
				.id(1L).nom(RoleEnum.ADMIN.name()).libelle("Patron").systemRole(false)
				.permissions(new HashSet<>(Set.of(PermissionEnum.DASHBOARD_READ)))
				.build();
		when(roleRepository.findByNom(RoleEnum.ADMIN.name())).thenReturn(Optional.of(adminExistant));

		seeder.run(null);

		assertThat(adminExistant.getPermissions()).hasSize(PermissionEnum.values().length);
		assertThat(adminExistant.getSystemRole()).isTrue();
		assertThat(adminExistant.getLibelle()).isEqualTo("Patron");
	}

	@Test
	@DisplayName("les utilisateurs sans aucun droit reçoivent ADMIN au premier démarrage")
	void backfillUtilisateursSansDroits() {
		Utilisateur orphelin = Utilisateur.builder().id(1L).email("a@ias.ci").build();
		when(userRepository.findAll()).thenReturn(List.of(orphelin));

		seeder.run(null);

		assertThat(orphelin.getRoles()).extracting(RoleEntity::getNom).containsExactly("ADMIN");
		verify(userRepository).saveAll(any());
	}

	@Test
	@DisplayName("la bascule ne se redéclenche pas dès qu'un droit existe quelque part")
	void backfillIgnoreSiUnDroitExiste() {
		Utilisateur avecRole = Utilisateur.builder().id(1L)
				.roles(new HashSet<>(Set.of(RoleEntity.builder().id(9L).nom("COMMERCIAL").build())))
				.build();
		Utilisateur sansRole = Utilisateur.builder().id(2L).build();
		when(userRepository.findAll()).thenReturn(List.of(avecRole, sansRole));

		seeder.run(null);

		assertThat(sansRole.getRoles()).isEmpty();
		verify(userRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("l'appartenance à un groupe suffit à considérer l'utilisateur comme doté de droits")
	void backfillIgnoreSiGroupe() {
		Utilisateur avecGroupe = Utilisateur.builder().id(1L)
				.groupes(new HashSet<>(Set.of(GroupeEntity.builder().id(5L).nom("FLOTTE").build())))
				.build();
		when(userRepository.findAll()).thenReturn(List.of(avecGroupe));

		seeder.run(null);

		assertThat(avecGroupe.getRoles()).isEmpty();
		verify(userRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("aucune bascule quand la base ne contient aucun utilisateur")
	void backfillIgnoreSiAucunUtilisateur() {
		seeder.run(null);

		verify(userRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("les rôles métier reçoivent un préréglage de permissions non vide et strictement inclus")
	void preréglagesMetier() {
		seeder.run(null);

		ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
		verify(roleRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

		captor.getAllValues().stream()
				.filter(r -> !"ADMIN".equals(r.getNom()))
				.forEach(role -> {
					assertThat(role.getPermissions()).as("rôle %s", role.getNom()).isNotEmpty();
					assertThat(role.getPermissions()).as("rôle %s", role.getNom())
							.hasSizeLessThan(PermissionEnum.values().length);
					assertThat(role.getPermissions()).as("rôle %s", role.getNom())
							.contains(PermissionEnum.DASHBOARD_READ);
				});
	}
}
