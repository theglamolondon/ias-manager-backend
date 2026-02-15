package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.core.LigneFacture;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneFactureRequest;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.entity.LigneFactureEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.*;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FactureService {

	private final FactureRepository factureRepository;
	private final LigneFactureRepository ligneFactureRepository;
	private final LivraisonClientRepository livraisonClientRepository;
	private final LivraisonFournisseurRepository livraisonFournisseurRepository;
	private final SortieProduitRepository sortieProduitRepository;
	private final EntreeProduitRepository entreeProduitRepository;
	private final PartenaireRepository partenaireRepository;
	private final ProduitRepository produitRepository;
	private final FactureMapper factureMapper;
	private final LigneFactureMapper ligneFactureMapper;
	private final LivraisonClientMapper livraisonClientMapper;
	private final LivraisonFournisseurMapper livraisonFournisseurMapper;
	private final SortieProduitMapper sortieProduitMapper;
	private final EntreeProduitMapper entreeProduitMapper;

	// ==================== FACTURES ====================

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getAllFactures(Pageable pageable) {
		return PagedResponse.of(factureRepository.findAll(pageable).map(this::toDtoWithItems));
	}

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getFacturesClients(Pageable pageable) {
		return PagedResponse.of(factureRepository.findByPartenaireIsClientTrue(pageable).map(this::toDtoWithItems));
	}

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getFacturesFournisseurs(Pageable pageable) {
		return PagedResponse.of(factureRepository.findByPartenaireIsFournisseurTrue(pageable).map(this::toDtoWithItems));
	}

	@Transactional(readOnly = true)
	public Facture getFactureById(Long id) {
		FactureEntity entity = factureRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Facture", id));
		return toDtoWithItems(entity);
	}

	@Transactional(readOnly = true)
	public Facture getFactureByNumProforma(String numProforma) {
		FactureEntity entity = factureRepository.findByNumProforma(numProforma)
				.orElseThrow(() -> new ResourceNotFoundException("Facture avec numéro proforma " + numProforma + " non trouvée"));
		return toDtoWithItems(entity);
	}

	@Transactional(readOnly = true)
	public Facture getFactureByNumFacture(String numFacture) {
		FactureEntity entity = factureRepository.findByNumFacture(numFacture)
				.orElseThrow(() -> new ResourceNotFoundException("Facture avec numéro facture " + numFacture + " non trouvée"));
		return toDtoWithItems(entity);
	}

	@Transactional
	public Facture createFacture(FactureRequest request) {
		FactureEntity entity = factureMapper.toEntity(request);
		entity.setStatut(FactureStatusEnum.PROFORMA);
		resolveRelations(request, entity);

		FactureEntity saved = factureRepository.save(entity);

		// Enregistrer les lignes
		List<LigneFactureEntity> lignes = saveLignes(saved, request.getItems());

		Facture dto = factureMapper.toDto(saved);
		dto.setItems(ligneFactureMapper.toDtoList(lignes));
		return dto;
	}

	@Transactional
	public Facture updateFacture(Long id, FactureRequest request) {
		FactureEntity entity = factureRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Facture", id));

		factureMapper.updateEntity(request, entity);
		resolveRelations(request, entity);

		FactureEntity saved = factureRepository.save(entity);
		return toDtoWithItems(saved);
	}

	@Transactional
	public void deleteFacture(Long id) {
		if (!factureRepository.existsById(id)) {
			throw new ResourceNotFoundException("Facture", id);
		}
		factureRepository.deleteById(id);
	}

	// ==================== LIGNES FACTURE ====================

	@Transactional(readOnly = true)
	public PagedResponse<LigneFacture> getLignesByFacture(Long factureId, Pageable pageable) {
		if (!factureRepository.existsById(factureId)) {
			throw new ResourceNotFoundException("Facture", factureId);
		}
		return PagedResponse.of(ligneFactureRepository.findByFactureId(factureId, pageable)
				.map(ligneFactureMapper::toDto));
	}

	@Transactional(readOnly = true)
	public LigneFacture getLigneById(Long factureId, Long ligneId) {
		if (!factureRepository.existsById(factureId)) {
			throw new ResourceNotFoundException("Facture", factureId);
		}
		LigneFactureEntity entity = ligneFactureRepository.findById(ligneId)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne facture", ligneId));

		if (!entity.getFacture().getId().equals(factureId)) {
			throw new ResourceNotFoundException("Ligne facture " + ligneId + " n'appartient pas à la facture " + factureId);
		}

		return ligneFactureMapper.toDto(entity);
	}

	@Transactional
	public LigneFacture createLigne(Long factureId, LigneFactureRequest request) {
		FactureEntity facture = factureRepository.findById(factureId)
				.orElseThrow(() -> new ResourceNotFoundException("Facture", factureId));

		LigneFactureEntity entity = ligneFactureMapper.toEntity(request);
		entity.setFacture(facture);
		resolveProduit(request, entity);

		LigneFactureEntity saved = ligneFactureRepository.save(entity);
		return ligneFactureMapper.toDto(saved);
	}

	@Transactional
	public LigneFacture updateLigne(Long factureId, Long ligneId, LigneFactureRequest request) {
		if (!factureRepository.existsById(factureId)) {
			throw new ResourceNotFoundException("Facture", factureId);
		}

		LigneFactureEntity entity = ligneFactureRepository.findById(ligneId)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne facture", ligneId));

		if (!entity.getFacture().getId().equals(factureId)) {
			throw new ResourceNotFoundException("Ligne facture " + ligneId + " n'appartient pas à la facture " + factureId);
		}

		ligneFactureMapper.updateEntity(request, entity);
		resolveProduit(request, entity);

		LigneFactureEntity saved = ligneFactureRepository.save(entity);
		return ligneFactureMapper.toDto(saved);
	}

	@Transactional
	public void deleteLigne(Long factureId, Long ligneId) {
		if (!factureRepository.existsById(factureId)) {
			throw new ResourceNotFoundException("Facture", factureId);
		}

		LigneFactureEntity entity = ligneFactureRepository.findById(ligneId)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne facture", ligneId));

		if (!entity.getFacture().getId().equals(factureId)) {
			throw new ResourceNotFoundException("Ligne facture " + ligneId + " n'appartient pas à la facture " + factureId);
		}

		ligneFactureRepository.deleteById(ligneId);
	}

	// ==================== CHANGEMENT DE STATUT ====================

	@Transactional
	public Facture changerStatut(Long id, FactureStatusEnum nouveauStatut) {
		FactureEntity entity = factureRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Facture", id));

		FactureStatusEnum statutActuel = entity.getStatut();
		validerTransition(statutActuel, nouveauStatut);

		entity.setStatut(nouveauStatut);
		FactureEntity saved = factureRepository.save(entity);
		return toDtoWithItems(saved);
	}

	private void validerTransition(FactureStatusEnum actuel, FactureStatusEnum nouveau) {
		boolean valide = switch (actuel) {
			case BROUILLON -> nouveau == FactureStatusEnum.PROFORMA || nouveau == FactureStatusEnum.ANNULEE;
			case PROFORMA -> nouveau == FactureStatusEnum.FACTUREE || nouveau == FactureStatusEnum.PAYEE || nouveau == FactureStatusEnum.ANNULEE;
			case FACTUREE -> nouveau == FactureStatusEnum.PAYEE;
			case PAYEE, ANNULEE -> false;
		};

		if (!valide) {
			throw new BadRequestException(
					String.format("Transition de statut invalide : %s → %s", actuel, nouveau));
		}
	}

	// ==================== HELPERS ====================

	private void resolveRelations(FactureRequest request, FactureEntity entity) {
		if (request.getPartenaireId() != null) {
			PartenaireEntity partenaire = partenaireRepository.findById(request.getPartenaireId())
					.orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getPartenaireId()));
			entity.setPartenaire(partenaire);
		}
	}

	private List<LigneFactureEntity> saveLignes(FactureEntity facture, List<LigneFactureRequest> items) {
		List<LigneFactureEntity> lignes = new ArrayList<>();
		if (items == null || items.isEmpty()) return lignes;

		for (LigneFactureRequest item : items) {
			LigneFactureEntity ligne = ligneFactureMapper.toEntity(item);
			ligne.setFacture(facture);
			resolveProduit(item, ligne);
			lignes.add(ligneFactureRepository.save(ligne));
		}
		return lignes;
	}

	private void resolveProduit(LigneFactureRequest request, LigneFactureEntity entity) {
		if (request.getProduitId() != null) {
			ProduitEntity produit = produitRepository.findById(request.getProduitId())
					.orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProduitId()));
			entity.setProduit(produit);
		} else {
			entity.setProduit(null);
		}
	}

	private Facture toDtoWithItems(FactureEntity entity) {
		Facture dto = factureMapper.toDto(entity);
		List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(entity.getId());
		dto.setItems(ligneFactureMapper.toDtoList(lignes));

		// Livraison client
		livraisonClientRepository.findByFactureId(entity.getId()).ifPresentOrElse(
				livraison -> {
					LivraisonClient livraisonDto = livraisonClientMapper.toDto(livraison);
					livraisonDto.setSorties(sortieProduitMapper.toDtoList(
							sortieProduitRepository.findByLivraisonClientId(livraison.getId())));
					dto.setLivraison(livraisonDto);
				},
				() -> {
					// Livraison fournisseur
					livraisonFournisseurRepository.findByFactureId(entity.getId()).ifPresent(livraison -> {
						LivraisonFournisseur livraisonDto = livraisonFournisseurMapper.toDto(livraison);
						livraisonDto.setEntrees(entreeProduitMapper.toDtoList(
								entreeProduitRepository.findByLivraisonFournisseurId(livraison.getId())));
						dto.setLivraison(livraisonDto);
					});
				}
		);

		return dto;
	}
}
