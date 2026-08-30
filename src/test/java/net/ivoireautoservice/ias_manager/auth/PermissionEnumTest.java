package net.ivoireautoservice.ias_manager.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PermissionEnum — cohérence du catalogue de permissions")
class PermissionEnumTest {

	private static final Set<String> MODULES_ATTENDUS = Set.of(
			"GENERAL", "VEHICULES", "RH", "STOCK", "FINANCES", "PARTENAIRES", "CONFIGURATION");

	@ParameterizedTest
	@EnumSource(PermissionEnum.class)
	@DisplayName("chaque permission déclare module, ressource et libellé")
	void metadonneesRenseignees(PermissionEnum permission) {
		assertThat(permission.getModule()).isNotBlank();
		assertThat(permission.getRessource()).isNotBlank();
		assertThat(permission.getLibelle()).isNotBlank();
	}

	@ParameterizedTest
	@EnumSource(PermissionEnum.class)
	@DisplayName("le module appartient à l'arborescence de menu connue du frontend")
	void moduleConnu(PermissionEnum permission) {
		assertThat(MODULES_ATTENDUS).contains(permission.getModule());
	}

	@Test
	@DisplayName("chaque ressource est rattachée à un seul module")
	void ressourceRattacheeAUnSeulModule() {
		var modulesParRessource = Arrays.stream(PermissionEnum.values())
				.collect(Collectors.groupingBy(PermissionEnum::getRessource,
						Collectors.mapping(PermissionEnum::getModule, Collectors.toSet())));

		assertThat(modulesParRessource).allSatisfy((ressource, modules) ->
				assertThat(modules).as("ressource %s", ressource).hasSize(1));
	}

	@Test
	@DisplayName("toute ressource expose au moins une permission de lecture (visibilité du menu)")
	void chaqueRessourceALectureOuGestion() {
		var parRessource = Arrays.stream(PermissionEnum.values())
				.collect(Collectors.groupingBy(PermissionEnum::getRessource));

		assertThat(parRessource).allSatisfy((ressource, permissions) ->
				assertThat(permissions)
						.as("ressource %s", ressource)
						.anySatisfy(p -> assertThat(p.name()).matches(".*_(READ|MANAGE)$")));
	}

	@Test
	@DisplayName("les noms de permission suivent la convention RESSOURCE_ACTION en majuscules")
	void conventionDeNommage() {
		assertThat(Arrays.stream(PermissionEnum.values()).map(Enum::name))
				.allMatch(nom -> nom.matches("[A-Z]+(_[A-Z]+)+"));
	}

	@Test
	@DisplayName("les libellés sont distincts au sein d'une même ressource")
	void libellesDistinctsParRessource() {
		var parRessource = Arrays.stream(PermissionEnum.values())
				.collect(Collectors.groupingBy(PermissionEnum::getRessource));

		assertThat(parRessource).allSatisfy((ressource, permissions) -> {
			var libelles = permissions.stream().map(PermissionEnum::getLibelle).collect(Collectors.toSet());
			assertThat(libelles).as("ressource %s", ressource).hasSize(permissions.size());
		});
	}
}
