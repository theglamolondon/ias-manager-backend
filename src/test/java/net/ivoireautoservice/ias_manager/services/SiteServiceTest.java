package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Site;
import net.ivoireautoservice.ias_manager.dto.request.SiteRequest;
import net.ivoireautoservice.ias_manager.entity.SiteEntity;
import net.ivoireautoservice.ias_manager.enums.LocalisationMissionEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.SiteMapper;
import net.ivoireautoservice.ias_manager.repository.SiteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteService — paramétrage général et suppléments de localisation")
class SiteServiceTest {

	@Mock
	private SiteRepository siteRepository;

	@Mock
	private SiteMapper siteMapper;

	@InjectMocks
	private SiteService service;

	private static SiteEntity site(Long id, BigDecimal interieur, BigDecimal exterieur) {
		return SiteEntity.builder()
				.id(id).raisonSociale("Ivoire Auto Services").devise("FCFA")
				.supIsInterieur(interieur).supIsExterieur(exterieur)
				.build();
	}

	@Nested
	@DisplayName("CRUD")
	class Crud {

		@Test
		@DisplayName("la liste paginée est projetée en PagedResponse")
		void getAllSites_pagine() {
			SiteEntity entity = site(1L, null, null);
			when(siteRepository.findAll(any(PageRequest.class)))
					.thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1));
			when(siteMapper.toDto(entity)).thenReturn(Site.builder().id(1L).build());

			var reponse = service.getAllSites(PageRequest.of(0, 10));

			assertThat(reponse.getContent()).hasSize(1);
			assertThat(reponse.getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("getSiteById lève 404 quand le site n'existe pas")
		void getSiteById_absent() {
			when(siteRepository.findById(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getSiteById(9L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Site avec l'id 9");
		}

		@Test
		@DisplayName("updateSite applique la requête sur l'entité existante")
		void updateSite() {
			SiteEntity entity = site(1L, null, null);
			SiteRequest request = SiteRequest.builder().raisonSociale("IAS SA").build();
			when(siteRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(siteRepository.save(entity)).thenReturn(entity);
			when(siteMapper.toDto(entity)).thenReturn(Site.builder().id(1L).build());

			service.updateSite(1L, request);

			verify(siteMapper).updateEntity(request, entity);
			verify(siteRepository).save(entity);
		}

		@Test
		@DisplayName("updateSite lève 404 sur un id inconnu")
		void updateSite_absent() {
			when(siteRepository.findById(9L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.updateSite(9L, SiteRequest.builder().build()))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("deleteSite lève 404 et ne supprime rien si le site est inconnu")
		void deleteSite_absent() {
			when(siteRepository.existsById(9L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteSite(9L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(siteRepository, never()).deleteById(any());
		}

		@Test
		@DisplayName("deleteSite supprime un site existant")
		void deleteSite() {
			when(siteRepository.existsById(1L)).thenReturn(true);

			service.deleteSite(1L);

			verify(siteRepository).deleteById(1L);
		}
	}

	@Nested
	@DisplayName("Site courant")
	class SiteCourant {

		@Test
		@DisplayName("retourne le premier site existant sans rien créer")
		void getCurrentSite_existant() {
			SiteEntity entity = site(1L, BigDecimal.TEN, BigDecimal.ONE);
			when(siteRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(entity));
			when(siteMapper.toDto(entity)).thenReturn(Site.builder().id(1L).build());

			service.getCurrentSite();

			verify(siteRepository, never()).save(any());
		}

		@Test
		@DisplayName("crée un site par défaut avec les suppléments standards quand la base est vide")
		void getCurrentSite_creeParDefaut() {
			when(siteRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
			when(siteRepository.save(any(SiteEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(siteMapper.toDto(any(SiteEntity.class))).thenReturn(Site.builder().build());

			service.getCurrentSite();

			ArgumentCaptor<SiteEntity> captor = ArgumentCaptor.forClass(SiteEntity.class);
			verify(siteRepository).save(captor.capture());
			assertThat(captor.getValue().getRaisonSociale()).isEqualTo("Ivoire Auto Services");
			assertThat(captor.getValue().getDevise()).isEqualTo("FCFA");
			assertThat(captor.getValue().getSupIsInterieur()).isEqualByComparingTo(SiteService.DEFAULT_SUP_INTERIEUR);
			assertThat(captor.getValue().getSupIsExterieur()).isEqualByComparingTo(SiteService.DEFAULT_SUP_EXTERIEUR);
		}
	}

	@Nested
	@DisplayName("Supplément journalier de localisation")
	class Supplement {

		@Test
		@DisplayName("VILLE ne donne aucun supplément et n'interroge pas la base")
		void ville() {
			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.VILLE))
					.isEqualByComparingTo(BigDecimal.ZERO);
			verify(siteRepository, never()).findFirstByOrderByIdAsc();
		}

		@Test
		@DisplayName("une localisation nulle est traitée comme VILLE")
		void localisationNulle() {
			assertThat(service.getSupplementJournalier(null)).isEqualByComparingTo(BigDecimal.ZERO);
		}

		@Test
		@DisplayName("INTERIEUR applique le supplément configuré sur le site")
		void interieurConfigure() {
			when(siteRepository.findFirstByOrderByIdAsc())
					.thenReturn(Optional.of(site(1L, BigDecimal.valueOf(7_500), BigDecimal.valueOf(20_000))));

			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.INTERIEUR))
					.isEqualByComparingTo(BigDecimal.valueOf(7_500));
		}

		@Test
		@DisplayName("EXTERIEUR applique le supplément configuré sur le site")
		void exterieurConfigure() {
			when(siteRepository.findFirstByOrderByIdAsc())
					.thenReturn(Optional.of(site(1L, BigDecimal.valueOf(7_500), BigDecimal.valueOf(20_000))));

			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.EXTERIEUR))
					.isEqualByComparingTo(BigDecimal.valueOf(20_000));
		}

		@Test
		@DisplayName("sans site en base, les valeurs par défaut s'appliquent")
		void sansSite() {
			when(siteRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.INTERIEUR))
					.isEqualByComparingTo(SiteService.DEFAULT_SUP_INTERIEUR);
			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.EXTERIEUR))
					.isEqualByComparingTo(SiteService.DEFAULT_SUP_EXTERIEUR);
		}

		@Test
		@DisplayName("un supplément non renseigné sur le site retombe sur la valeur par défaut")
		void supplementNull() {
			when(siteRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(site(1L, null, null)));

			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.INTERIEUR))
					.isEqualByComparingTo(SiteService.DEFAULT_SUP_INTERIEUR);
			assertThat(service.getSupplementJournalier(LocalisationMissionEnum.EXTERIEUR))
					.isEqualByComparingTo(SiteService.DEFAULT_SUP_EXTERIEUR);
		}
	}
}
