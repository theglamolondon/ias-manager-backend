package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Site;
import net.ivoireautoservice.ias_manager.dto.request.SiteRequest;
import net.ivoireautoservice.ias_manager.entity.SiteEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.SiteMapper;
import net.ivoireautoservice.ias_manager.repository.SiteRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SiteService {

	public static final BigDecimal DEFAULT_SUP_INTERIEUR = BigDecimal.valueOf(10_000);
	public static final BigDecimal DEFAULT_SUP_EXTERIEUR = BigDecimal.valueOf(15_000);

	private final SiteRepository siteRepository;
	private final SiteMapper siteMapper;

	@Transactional(readOnly = true)
	public PagedResponse<Site> getAllSites(Pageable pageable) {
		return PagedResponse.of(siteRepository.findAll(pageable).map(siteMapper::toDto));
	}

	@Transactional(readOnly = true)
	public Site getSiteById(Long id) {
		SiteEntity entity = siteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Site", id));
		return siteMapper.toDto(entity);
	}

	@Transactional
	public Site getCurrentSite() {
		SiteEntity entity = siteRepository.findFirstByOrderByIdAsc()
				.orElseGet(() -> siteRepository.save(SiteEntity.builder()
						.raisonSociale("Ivoire Auto Services")
						.devise("FCFA")
						.supIsInterieur(DEFAULT_SUP_INTERIEUR)
						.supIsExterieur(DEFAULT_SUP_EXTERIEUR)
						.build()));
		return siteMapper.toDto(entity);
	}

	@Transactional
	public Site createSite(SiteRequest request) {
		SiteEntity entity = siteMapper.toEntity(request);
		return siteMapper.toDto(siteRepository.save(entity));
	}

	@Transactional
	public Site updateSite(Long id, SiteRequest request) {
		SiteEntity entity = siteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Site", id));
		siteMapper.updateEntity(request, entity);
		return siteMapper.toDto(siteRepository.save(entity));
	}

	@Transactional
	public void deleteSite(Long id) {
		if (!siteRepository.existsById(id)) {
			throw new ResourceNotFoundException("Site", id);
		}
		siteRepository.deleteById(id);
	}

	/**
	 * Retourne le supplément journalier configuré pour les missions selon leur localisation.
	 * Valeurs par défaut : 10 000 FCFA en intérieur, 15 000 FCFA en extérieur.
	 */
	@Transactional(readOnly = true)
	public BigDecimal getSupplementJournalier(Boolean isInterieur) {
		SiteEntity site = siteRepository.findFirstByOrderByIdAsc().orElse(null);
		if (Boolean.TRUE.equals(isInterieur)) {
			return site != null && site.getSupIsInterieur() != null ? site.getSupIsInterieur() : DEFAULT_SUP_INTERIEUR;
		}
		return site != null && site.getSupIsExterieur() != null ? site.getSupIsExterieur() : DEFAULT_SUP_EXTERIEUR;
	}
}
