package net.ivoireautoservice.ias_manager.entity;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Utilisateur — dérivation RBAC des rôles et permissions")
class UtilisateurRbacTest {

	private static RoleEntity role(Long id, String nom, PermissionEnum... permissions) {
		return RoleEntity.builder()
				.id(id)
				.nom(nom)
				.permissions(permissions.length == 0
						? EnumSet.noneOf(PermissionEnum.class)
						: EnumSet.copyOf(Set.of(permissions)))
				.build();
	}

	private static GroupeEntity groupe(Long id, String nom, RoleEntity... roles) {
		return GroupeEntity.builder().id(id).nom(nom).roles(Set.of(roles)).build();
	}

	@Nested
	@DisplayName("Rôles effectifs")
	class RolesEffectifs {

		@Test
		@DisplayName("union des rôles directs et des rôles hérités des groupes")
		void union() {
			RoleEntity direct = role(1L, "COMMERCIAL");
			RoleEntity herite = role(2L, "RECOUVREMENT");
			Utilisateur user = Utilisateur.builder()
					.roles(Set.of(direct))
					.groupes(Set.of(groupe(10L, "FINANCE", herite)))
					.build();

			assertThat(user.getEffectiveRoles()).containsExactlyInAnyOrder(direct, herite);
			assertThat(user.getRoleNames()).containsExactly("COMMERCIAL", "RECOUVREMENT");
		}

		@Test
		@DisplayName("un rôle à la fois direct et hérité n'est compté qu'une fois")
		void deduplicationParId() {
			RoleEntity partage = role(1L, "COMMERCIAL");
			Utilisateur user = Utilisateur.builder()
					.roles(Set.of(partage))
					.groupes(Set.of(groupe(10L, "VENTES", role(1L, "COMMERCIAL"))))
					.build();

			assertThat(user.getEffectiveRoles()).hasSize(1);
		}

		@Test
		@DisplayName("tolère des collections nulles (construction hors builder)")
		void collectionsNulles() {
			Utilisateur user = new Utilisateur();
			user.setRoles(null);
			user.setGroupes(null);

			assertThat(user.getEffectiveRoles()).isEmpty();
			assertThat(user.getRoleNames()).isEmpty();
			assertThat(user.getGroupeNames()).isEmpty();
			assertThat(user.getRoleIds()).isEmpty();
			assertThat(user.getGroupeIds()).isEmpty();
			assertThat(user.getPermissionNames()).isEmpty();
			assertThat(user.getPrimaryRoleName()).isNull();
		}
	}

	@Nested
	@DisplayName("Identifiants exposés à l'UI")
	class Identifiants {

		@Test
		@DisplayName("getRoleIds ne retient que les rôles directs, hors héritage")
		void roleIds_horsHeritage() {
			Utilisateur user = Utilisateur.builder()
					.roles(Set.of(role(1L, "COMMERCIAL")))
					.groupes(Set.of(groupe(10L, "FINANCE", role(2L, "RECOUVREMENT"))))
					.build();

			assertThat(user.getRoleIds()).containsExactly(1L);
			assertThat(user.getGroupeIds()).containsExactly(10L);
			assertThat(user.getGroupeNames()).containsExactly("FINANCE");
		}

		@Test
		@DisplayName("le rôle principal est le premier par ordre alphabétique")
		void primaryRoleName() {
			Utilisateur user = Utilisateur.builder()
					.roles(Set.of(role(1L, "RECOUVREMENT"), role(2L, "ACHAT")))
					.build();

			assertThat(user.getPrimaryRoleName()).isEqualTo("ACHAT");
		}
	}

	@Nested
	@DisplayName("Autorités Spring Security")
	class Autorites {

		@Test
		@DisplayName("chaque rôle produit ROLE_<nom> et une autorité par permission")
		void autorites() {
			Utilisateur user = Utilisateur.builder()
					.roles(Set.of(role(1L, "LOGISTIQUE",
							PermissionEnum.VEHICULE_READ, PermissionEnum.MISSION_READ)))
					.build();

			assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
					.containsExactlyInAnyOrder("ROLE_LOGISTIQUE", "VEHICULE_READ", "MISSION_READ");
		}

		@Test
		@DisplayName("les permissions héritées d'un groupe sont bien des autorités")
		void autoritesHeritees() {
			Utilisateur user = Utilisateur.builder()
					.groupes(Set.of(groupe(10L, "FLOTTE",
							role(1L, "LOGISTIQUE", PermissionEnum.VEHICULE_UPDATE))))
					.build();

			assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
					.contains("VEHICULE_UPDATE", "ROLE_LOGISTIQUE");
		}

		@Test
		@DisplayName("un rôle sans permission ne produit que son autorité ROLE_")
		void roleSansPermission() {
			Utilisateur user = Utilisateur.builder().roles(Set.of(role(1L, "VIDE"))).build();

			assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
					.containsExactly("ROLE_VIDE");
		}

		@Test
		@DisplayName("les permissions effectives sont dédoublonnées et triées")
		void permissionNames() {
			Utilisateur user = Utilisateur.builder()
					.roles(Set.of(role(1L, "A", PermissionEnum.VEHICULE_READ)))
					.groupes(Set.of(groupe(10L, "G",
							role(2L, "B", PermissionEnum.VEHICULE_READ, PermissionEnum.DASHBOARD_READ))))
					.build();

			assertThat(user.getPermissionNames())
					.containsExactly("DASHBOARD_READ", "VEHICULE_READ");
		}
	}

	@Nested
	@DisplayName("Contrat UserDetails")
	class ContratUserDetails {

		@Test
		@DisplayName("le nom d'utilisateur est l'email et le compte est toujours actif")
		void contrat() {
			Utilisateur user = Utilisateur.builder()
					.email("agent@ias.ci").password("hash").build();

			assertThat(user.getUsername()).isEqualTo("agent@ias.ci");
			assertThat(user.getPassword()).isEqualTo("hash");
			assertThat(user.isEnabled()).isTrue();
			assertThat(user.isAccountNonExpired()).isTrue();
			assertThat(user.isAccountNonLocked()).isTrue();
			assertThat(user.isCredentialsNonExpired()).isTrue();
		}
	}
}
