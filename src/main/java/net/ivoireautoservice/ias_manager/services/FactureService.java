package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.config.MoneyUtils;
import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.core.LigneFacture;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.FactureFournisseurGroupeeRequest;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneFactureRequest;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.EntreeProduitEntity;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.entity.LigneBonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.LigneFactureEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureNatureEnum;
import net.ivoireautoservice.ias_manager.enums.FactureTypeEnum;
import net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum;
import net.ivoireautoservice.ias_manager.enums.TypePartenaireEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.*;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final CompteService compteService;
	private final PrintService printService;
	private final MoneyUtils moneyUtils;

	// ==================== FACTURES ====================

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getAllFactures(String keyword, Boolean factureClient, Pageable pageable) {
		if (keyword != null && !keyword.isBlank()) {
			return PagedResponse.of(factureRepository.searchByKeyword(keyword.trim(), factureClient, pageable).map(this::toDtoWithItems));
		}
		if (factureClient != null) {
			return PagedResponse.of(factureRepository.findByFactureClient(factureClient, pageable).map(this::toDtoWithItems));
		}
		return PagedResponse.of(factureRepository.findAll(pageable).map(this::toDtoWithItems));
	}

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getFacturesClients(Pageable pageable) {
		return PagedResponse.of(factureRepository.findByFactureClient(true, pageable).map(this::toDtoWithItems));
	}

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getFacturesFournisseurs(Pageable pageable) {
		return PagedResponse.of(factureRepository.findByFactureClient(false, pageable).map(this::toDtoWithItems));
	}

	@Transactional(readOnly = true)
	public PagedResponse<Facture> getFacturesLivrables(Boolean factureClient, Pageable pageable) {
		List<FactureStatusEnum> statuts = List.of(FactureStatusEnum.PROFORMA, FactureStatusEnum.PAYEE);
		if (factureClient != null) {
			return PagedResponse.of(factureRepository.findFacturesSansLivraison(statuts, factureClient, pageable).map(this::toDtoWithItems));
		}
		return PagedResponse.of(factureRepository.findFacturesSansLivraison(statuts, pageable).map(this::toDtoWithItems));
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
		return createFacture(request, FactureTypeEnum.PRODUIT);
	}

	@Transactional
	public Facture createFacture(FactureRequest request, FactureTypeEnum type) {
		FactureEntity entity = factureMapper.toEntity(request);
		entity.setStatut(FactureStatusEnum.PROFORMA);
		entity.setType(type);
		resolveRelations(request, entity);

		// Forcer factureClient selon le contexte (pas selon le type du partenaire)
		// Le champ factureClient du request est la source de vérité
		entity.setFactureClient(Boolean.TRUE.equals(request.getFactureClient()));

		// Génération auto du numéro proforma fournisseur si absent : format DA/01/79/{seq}
		if (Boolean.FALSE.equals(entity.getFactureClient())
				&& (entity.getNumProforma() == null || entity.getNumProforma().isBlank())) {
			entity.setNumProforma(generateNumProformaFournisseur());
		}

		// Déterminer la nature selon le type du partenaire
		if (entity.getPartenaire() != null && entity.getPartenaire().getType() == TypePartenaireEnum.PARTICULIER) {
			entity.setNature(FactureNatureEnum.RECU);
		} else {
			entity.setNature(FactureNatureEnum.FACTURE);
		}
		if(request.getFactureClient() != null && Boolean.FALSE.equals(request.getFactureClient())) {
			entity.setNature(FactureNatureEnum.FACTURE);
		}

		// Déterminer le type : PRODUIT si au moins une ligne a un produit associé, sinon AUTRE
		if (type != FactureTypeEnum.MISSION) {
			boolean hasProduit = request.getItems() != null && request.getItems().stream()
					.anyMatch(item -> item.getProduitId() != null);
			entity.setType(hasProduit ? FactureTypeEnum.PRODUIT : FactureTypeEnum.AUTRE);
		}

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

		if (entity.getStatut() == FactureStatusEnum.ANNULEE) {
			throw new BadRequestException("Impossible de modifier une facture annulée");
		}
		if (entity.getStatut() == FactureStatusEnum.PAYEE) {
			throw new BadRequestException("Impossible de modifier une facture payée");
		}

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

	// ==================== FACTURATION GROUPÉE FOURNISSEUR ====================

	@Transactional
	public Facture genererFactureFournisseurGroupee(FactureFournisseurGroupeeRequest request) {
		List<Long> livraisonIds = request.getLivraisonIds();
		List<LivraisonFournisseurEntity> livraisons = livraisonFournisseurRepository.findAllById(livraisonIds);

		if (livraisons.size() != livraisonIds.size()) {
			throw new BadRequestException("Un ou plusieurs bons de livraison sont introuvables");
		}

		// Validation : tous VALIDE, non facturés, même partenaire
		PartenaireEntity partenaire = null;
		Float tva = null;
		for (LivraisonFournisseurEntity bl : livraisons) {
			if (bl.getStatut() != StatutBonLivraisonEnum.VALIDE) {
				throw new BadRequestException("Le bon de livraison " + bl.getNumero()
						+ " n'est pas VALIDE (statut actuel : " + bl.getStatut() + ")");
			}
			if (bl.getFacture() != null) {
				throw new BadRequestException("Le bon de livraison " + bl.getNumero()
						+ " est déjà rattaché à la facture " + bl.getFacture().getNumFacture());
			}
			BonCommandeEntity bc = bl.getBonCommande();
			if (bc == null || bc.getPartenaire() == null) {
				throw new BadRequestException("Bon de livraison " + bl.getNumero() + " sans partenaire identifiable");
			}
			if (partenaire == null) {
				partenaire = bc.getPartenaire();
				tva = bc.getTva() != null ? bc.getTva() : 0f;
			} else if (!partenaire.getId().equals(bc.getPartenaire().getId())) {
				throw new BadRequestException("Tous les bons de livraison doivent provenir du même fournisseur");
			} else {
				// TVA homogène : on prend la TVA du premier BC, et on refuse une divergence majeure.
				float tvaCourante = bc.getTva() != null ? bc.getTva() : 0f;
				if (Math.abs(tvaCourante - tva) > 0.001f) {
					throw new BadRequestException("TVA hétérogène entre les bons de commande : "
							+ tva + "% vs " + tvaCourante + "%. Facturez-les séparément.");
				}
			}
		}

		// Construction de la facture
		FactureEntity facture = FactureEntity.builder()
				.factureClient(false)
				.statut(FactureStatusEnum.FACTUREE)
				.nature(FactureNatureEnum.FACTURE)
				.type(FactureTypeEnum.PRODUIT)
				.tva(tva)
				.partenaire(partenaire)
				.objet(request.getObjet() != null && !request.getObjet().isBlank()
						? request.getObjet()
						: "Facture fournisseur groupée — " + livraisons.size() + " livraison(s)")
				.numProforma(generateNumProformaFournisseur())
				.numFacture(generateNumFactureGroupee())
				.build();
		FactureEntity savedFacture = factureRepository.save(facture);

		// Création des lignes à partir des entrées de chaque BL
		long montantHt = 0L;
		for (LivraisonFournisseurEntity bl : livraisons) {
			List<EntreeProduitEntity> entrees = entreeProduitRepository.findByLivraisonFournisseurId(bl.getId());
			for (EntreeProduitEntity entree : entrees) {
				LigneBonCommandeEntity ligneBc = entree.getLigneBonCommande();
				if (ligneBc == null) continue;
				long prixUnitaire = ligneBc.getPrixUnitaire() != null ? ligneBc.getPrixUnitaire() : 0L;
				float remise = ligneBc.getRemise() != null ? ligneBc.getRemise() : 0f;
				long ligneHt = Math.round(entree.getQuantite() * prixUnitaire * (1 - remise / 100f));
				montantHt += ligneHt;

				LigneFactureEntity ligneFacture = LigneFactureEntity.builder()
						.reference(ligneBc.getReference())
						.designation(ligneBc.getDesignation())
						.qte(entree.getQuantite())
						.prixUnitaire(prixUnitaire)
						.remise(remise)
						.montantHt(ligneHt)
						.extraRef(ligneBc.getExtraRef())
						.facture(savedFacture)
						.produit(ligneBc.getProduit())
						.build();
				ligneFactureRepository.save(ligneFacture);
			}

			// Liaison BL → facture
			bl.setFacture(savedFacture);
			livraisonFournisseurRepository.save(bl);
		}

		savedFacture.setMontantHt(montantHt);
		savedFacture.setMontantTtc(Math.round(montantHt * (1.0 + (tva != null ? tva : 0f) / 100.0)));
		FactureEntity finalFacture = factureRepository.save(savedFacture);
		return toDtoWithItems(finalFacture);
	}

	private String generateNumFactureGroupee() {
		return "FG-" + System.currentTimeMillis();
	}

	/**
	 * Génère un numéro proforma fournisseur au format {PREFIX}{seq} où PREFIX = "DA/01/79/".
	 */
	private static final String NUM_PROFORMA_FOURNISSEUR_PREFIX = "DA/01/79/";

	private String generateNumProformaFournisseur() {
		Integer maxSuffix = factureRepository.findMaxNumProformaSuffix(NUM_PROFORMA_FOURNISSEUR_PREFIX);
		int next = (maxSuffix != null ? maxSuffix : 0) + 1;
		return NUM_PROFORMA_FOURNISSEUR_PREFIX + next;
	}

	// ==================== CHANGEMENT DE STATUT ====================

	@Transactional
	public Facture changerStatut(Long id, FactureStatusEnum nouveauStatut, Long compteId) {
		FactureEntity entity = factureRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Facture", id));

		FactureStatusEnum statutActuel = entity.getStatut();
		validerTransition(statutActuel, nouveauStatut);

		// Si annulation d'une facture fournisseur, vérifier qu'aucune livraison n'est liée (modèle 1:N)
		if (nouveauStatut == FactureStatusEnum.ANNULEE && Boolean.FALSE.equals(entity.getFactureClient())) {
			List<LivraisonFournisseurEntity> blsLies = livraisonFournisseurRepository.findAllByFactureId(id);
			if (!blsLies.isEmpty()) {
				String numeros = blsLies.stream().map(LivraisonFournisseurEntity::getNumero).reduce((a, b) -> a + ", " + b).orElse("");
				throw new BadRequestException("Impossible d'annuler cette facture : "
						+ blsLies.size() + " bon(s) de livraison fournisseur y sont liés (" + numeros + ")");
			}
		}

		// Si passage à PAYEE, le compteId est obligatoire
		if (nouveauStatut == FactureStatusEnum.PAYEE) {
			if (compteId == null) {
				throw new BadRequestException("Le compte est obligatoire pour marquer une facture comme payée");
			}
			enregistrerMouvementCompte(entity, compteId);
		}

		entity.setStatut(nouveauStatut);
		FactureEntity saved = factureRepository.save(entity);
		return toDtoWithItems(saved);
	}

	private void enregistrerMouvementCompte(FactureEntity facture, Long compteId) {
		boolean isClient = Boolean.TRUE.equals(facture.getFactureClient());
		String numRef = facture.getNumFacture() != null ? facture.getNumFacture() : facture.getNumProforma();

		CompteLigneType type = isClient ? CompteLigneType.APPROVISIONNEMENT : CompteLigneType.DEPENSE;
		String objet = isClient
				? "ENCAISSEMENT FACTURE N°" + numRef
				: "PAIEMENT FACTURE N°" + numRef;

		LigneCompteRequest ligneRequest = LigneCompteRequest.builder()
				.type(type)
				.objet(objet)
				.montant(facture.getMontantTtc())
				.observation("Facture " + numRef + " — " + (facture.getObjet() != null ? facture.getObjet() : ""))
				.build();

		compteService.createLigne(compteId, ligneRequest);
	}

	private void validerTransition(FactureStatusEnum actuel, FactureStatusEnum nouveau) {
		boolean valide = switch (actuel) {
			case BROUILLON -> nouveau == FactureStatusEnum.PROFORMA || nouveau == FactureStatusEnum.ANNULEE;
			case PROFORMA -> nouveau == FactureStatusEnum.FACTUREE || nouveau == FactureStatusEnum.PAYEE || nouveau == FactureStatusEnum.ANNULEE;
			case FACTUREE -> nouveau == FactureStatusEnum.PAYEE || nouveau == FactureStatusEnum.ANNULEE;
			case PAYEE, ANNULEE -> false;
		};

		if (!valide) {
			throw new BadRequestException(
					String.format("Transition de statut invalide : %s → %s", actuel, nouveau));
		}
	}

	// ==================== PDF ====================

	@Transactional(readOnly = true)
	public byte[] generatePdf(Long numero) {
		FactureEntity entity = factureRepository.findById(numero)
				.orElseThrow(() -> new ResourceNotFoundException("Facture avec numero " + numero + " non trouvee"));

		Facture facture = toDtoWithItems(entity);
		PartenaireEntity partenaire = entity.getPartenaire();

		Map<String, Object> data = new HashMap<>();
		data.put("facture", facture);
		data.put("partenaire", partenaire);
		data.put("montantEnLettres", moneyUtils.montantEnLettre(facture.getMontantTtc()));
		data.put("logoUrl", "classpath:/static/img/logo-ias.png");

		String template = entity.getType() == FactureTypeEnum.MISSION
				? "pdf/factureLocation"
				: "pdf/factureProforma";
		return printService.generatePdf(template, data);
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
					// Livraison fournisseur — modèle 1:N depuis la facturation groupée.
					// On expose ici la première livraison liée pour rétro-compatibilité du DTO.
					List<LivraisonFournisseurEntity> bls = livraisonFournisseurRepository.findAllByFactureId(entity.getId());
					if (!bls.isEmpty()) {
						LivraisonFournisseurEntity livraison = bls.get(0);
						LivraisonFournisseur livraisonDto = livraisonFournisseurMapper.toDto(livraison);
						livraisonDto.setEntrees(entreeProduitMapper.toDtoList(
								entreeProduitRepository.findByLivraisonFournisseurId(livraison.getId())));
						dto.setLivraison(livraisonDto);
					}
				}
		);

		return dto;
	}
}
