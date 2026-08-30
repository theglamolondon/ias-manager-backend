package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Groupe;
import net.ivoireautoservice.ias_manager.dto.request.GroupeRequest;
import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.GroupeMapper;
import net.ivoireautoservice.ias_manager.repository.GroupeRepository;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupeService — groupes de rôles")
class GroupeServiceTest {

	@Mock
	private GroupeRepository groupeRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private GroupeMapper groupeMapper;

	@InjectMocks
	private GroupeService service;

	@Test
	@DisplayName("la création normalise le nom et résout les rôles demandés")
	void create_resoutLesRoles() {
		RoleEntity role = RoleEntity.builder().id(2L).nom("COMMERCIAL").build();
		GroupeRequest request = GroupeRequest.builder().nom("  VENTES ").roleIds(Set.of(2L)).build();
		GroupeEntity entity = new GroupeEntity();
		when(groupeRepository.existsByNom("VENTES")).thenReturn(false);
		when(groupeMapper.toEntity(request)).thenReturn(entity);
		when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
		when(groupeRepository.save(entity)).thenReturn(entity);
		when(groupeMapper.toDto(entity)).thenReturn(Groupe.builder().build());

		service.createGroupe(request);

		assertThat(entity.getNom()).isEqualTo("VENTES");
		assertThat(entity.getRoles()).containsExactly(role);
	}

	@Test
	@DisplayName("la création refuse un nom déjà pris")
	void create_nomDuplique() {
		when(groupeRepository.existsByNom("VENTES")).thenReturn(true);

		assertThatThrownBy(() -> service.createGroupe(GroupeRequest.builder().nom("VENTES").build()))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("existe déjà");
		verify(groupeRepository, never()).save(any());
	}

	@Test
	@DisplayName("la création échoue si un rôle référencé n'existe pas")
	void create_roleInconnu() {
		GroupeRequest request = GroupeRequest.builder().nom("VENTES").roleIds(Set.of(99L)).build();
		when(groupeRepository.existsByNom("VENTES")).thenReturn(false);
		when(groupeMapper.toEntity(request)).thenReturn(new GroupeEntity());
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createGroupe(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Rôle avec l'id 99");
	}

	@Test
	@DisplayName("un groupe sans rôle est accepté")
	void create_sansRole() {
		GroupeRequest request = GroupeRequest.builder().nom("OBSERVATEURS").roleIds(Set.of()).build();
		GroupeEntity entity = new GroupeEntity();
		when(groupeRepository.existsByNom("OBSERVATEURS")).thenReturn(false);
		when(groupeMapper.toEntity(request)).thenReturn(entity);
		when(groupeRepository.save(entity)).thenReturn(entity);
		when(groupeMapper.toDto(entity)).thenReturn(Groupe.builder().build());

		service.createGroupe(request);

		assertThat(entity.getRoles()).isEmpty();
		verify(roleRepository, never()).findById(any());
	}

	@Test
	@DisplayName("la mise à jour remplace intégralement l'ensemble des rôles")
	void update_remplaceLesRoles() {
		RoleEntity ancien = RoleEntity.builder().id(1L).nom("ANCIEN").build();
		RoleEntity nouveau = RoleEntity.builder().id(2L).nom("NOUVEAU").build();
		GroupeEntity entity = GroupeEntity.builder().id(10L).nom("VENTES")
				.roles(new java.util.HashSet<>(Set.of(ancien))).build();
		GroupeRequest request = GroupeRequest.builder().nom("VENTES").roleIds(Set.of(2L)).build();
		when(groupeRepository.findById(10L)).thenReturn(Optional.of(entity));
		when(roleRepository.findById(2L)).thenReturn(Optional.of(nouveau));
		when(groupeRepository.save(entity)).thenReturn(entity);
		when(groupeMapper.toDto(entity)).thenReturn(Groupe.builder().build());

		service.updateGroupe(10L, request);

		assertThat(entity.getRoles()).containsExactly(nouveau);
	}

	@Test
	@DisplayName("la mise à jour tolère de conserver le même nom")
	void update_memeNom() {
		GroupeEntity entity = GroupeEntity.builder().id(10L).nom("VENTES").build();
		GroupeRequest request = GroupeRequest.builder().nom("VENTES").build();
		when(groupeRepository.findById(10L)).thenReturn(Optional.of(entity));
		when(groupeRepository.save(entity)).thenReturn(entity);
		when(groupeMapper.toDto(entity)).thenReturn(Groupe.builder().build());

		service.updateGroupe(10L, request);

		verify(groupeRepository, never()).existsByNom(any());
	}

	@Test
	@DisplayName("la mise à jour refuse un renommage vers un nom déjà pris")
	void update_nomDuplique() {
		GroupeEntity entity = GroupeEntity.builder().id(10L).nom("VENTES").build();
		when(groupeRepository.findById(10L)).thenReturn(Optional.of(entity));
		when(groupeRepository.existsByNom("FINANCE")).thenReturn(true);

		assertThatThrownBy(() -> service.updateGroupe(10L, GroupeRequest.builder().nom("FINANCE").build()))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	@DisplayName("la suppression lève 404 sur un id inconnu")
	void delete_absent() {
		when(groupeRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteGroupe(99L))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(groupeRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("getGroupeById lève 404 sur un id inconnu")
	void getById_absent() {
		when(groupeRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getGroupeById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Groupe avec l'id 99");
	}
}
