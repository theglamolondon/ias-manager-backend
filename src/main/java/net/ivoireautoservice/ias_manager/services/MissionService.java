package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.AffecterChauffeurRequest;
import net.ivoireautoservice.ias_manager.dto.request.AnnulerMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.ChangerVehiculeMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneFactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.FactureNatureEnum;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureTypeEnum;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.LocalisationMissionEnum;
import net.ivoireautoservice.ias_manager.enums.MissionStatutFilter;
import net.ivoireautoservice.ias_manager.enums.PhotoMissionTypeEnum;
import net.ivoireautoservice.ias_manager.enums.StatutChauffeurEnum;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import java.util.Objects;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.DepenseMissionMapper;
import net.ivoireautoservice.ias_manager.mapper.FactureMapper;
import net.ivoireautoservice.ias_manager.mapper.MediaMapper;
import net.ivoireautoservice.ias_manager.mapper.MissionMapper;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

	private final MissionRepository missionRepository;
	private final DepenseMissionRepository depenseMissionRepository;
	private final PhotoMissionRepository photoMissionRepository;
	private final VehiculeRepository vehiculeRepository;
	private final ChauffeurRepository chauffeurRepository;
	private final PartenaireRepository partenaireRepository;
	private final TypeDepenseRepository typeDepenseRepository;
	private final FactureRepository factureRepository;
	private final LigneCompteRepository ligneCompteRepository;
	private final InterventionRepository interventionRepository;
	private final MissionMapper missionMapper;
	private final FactureMapper factureMapper;
	private final DepenseMissionMapper depenseMissionMapper;
	private final MediaMapper mediaMapper;
	private final MediaService mediaService;
	private final FactureService factureService;
	private final CompteService compteService;
	private final SiteService siteService;

	// ==================== MISSIONS ====================

	@Transactional(readOnly = true)
	public PagedResponse<Mission> getAllMissions(String keyword, MissionStatutFilter statut, Long partenaireId, Pageable pageable) {
		String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
		String statutParam = statut != null ? statut.name() : null;
		Page<MissionEntity> page = (keywordParam == null && statutParam == null && partenaireId == null)
				? missionRepository.findAll(pageable)
				: missionRepository.search(keywordParam, statutParam, partenaireId, pageable);

		List<String> codes = page.stream()
				.map(MissionEntity::getCodeMission)
				.filter(c -> c != null && !c.isBlank())
				.toList();
		// Lien mission ↔ facture par LigneFacture.extraRef (= codeMission), depuis
		// que la numérotation facture est passée au format DA/01/79/{seq}.
		java.util.Map<String, FactureEntity> facturesByCode = new java.util.HashMap<>();
		if (!codes.isEmpty()) {
			for (Object[] row : factureRepository.findFacturesByLigneExtraRefIn(codes)) {
				String code = (String) row[0];
				FactureEntity facture = (FactureEntity) row[1];
				facturesByCode.putIfAbsent(code, facture);
			}
		}

		return PagedResponse.of(page.map(entity -> {
			Mission dto = missionMapper.toDto(entity);
			FactureEntity facture = facturesByCode.get(entity.getCodeMission());
			if (facture != null) dto.setFacture(factureMapper.toDto(facture));
			return dto;
		}));
	}

	/**
	 * Missions facturables manuellement pour un client donné : toutes les
	 * missions non annulées qui ne sont pas déjà rattachées à une facture,
	 * tous types de tarification confondus. Utilisé pour la facturation
	 * groupée depuis le module Factures Client.
	 */
	@Transactional(readOnly = true)
	public List<Mission> getMissionsFacturables(Long clientId) {
		List<MissionEntity> missions = missionRepository.findFacturablesByClient(clientId);
		return missionMapper.toDtoList(missions);
	}

	/**
	 * Retourne toutes les factures émises pour une mission donnée (mission
	 * classique auto-facturée à la création, ou facturation groupée
	 * multi-missions depuis le module Factures Client).
	 */
	@Transactional(readOnly = true)
	public List<Facture> getFacturesByMissionId(Long missionId) {
		MissionEntity mission = missionRepository.findById(missionId)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));
		return factureService.getFacturesByCodeMission(mission.getCodeMission());
	}

	@Transactional(readOnly = true)
	public Mission getMissionById(Long id) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));
		Mission dto = missionMapper.toDto(entity);

		List<DepenseMissionEntity> depenses = depenseMissionRepository.findByMissionId(id);
		dto.setDepenses(depenseMissionMapper.toDtoList(depenses));

		List<PhotoMissionEntity> photoEntities = photoMissionRepository.findByMissionId(id);
		List<PhotoMission> photos = photoEntities.stream()
				.map(p -> PhotoMission.builder()
						.id(p.getId())
						.type(p.getType())
						.media(mediaMapper.toDto(p.getMedia()))
						.build())
				.toList();
		dto.setPhotos(photos);

		// La facture associée à une mission est retrouvée via LigneFacture.extraRef.
		if (entity.getCodeMission() != null) {
			factureRepository.findByLigneExtraRef(entity.getCodeMission()).stream()
					.findFirst()
					.ifPresent(facture -> dto.setFacture(factureMapper.toDto(facture)));
		}

		return dto;
	}

	@Transactional
	public Mission createMission(MissionRequest request) {
		// Validation sous-traitance
		if (Boolean.TRUE.equals(request.getIsSousTraitee())) {
			if (request.getDetailsVehiculeSousTraitance() == null || request.getDetailsVehiculeSousTraitance().isBlank()) {
				throw new BadRequestException("Les détails du véhicule de sous-traitance sont obligatoires pour une mission sous-traitée");
			}
		}

		VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));

		MissionEntity entity = missionMapper.toEntity(request);
		entity.setVehicule(vehicule);
		resolveRelations(request, entity);

		// Champs non renseignés à la création
		entity.setDhmsDebutReel(null);
		entity.setDhmsFinReel(null);
		entity.setKilometrageArrive(null);

		// Génération du code mission : YYYY-NNN
		int year = LocalDate.now().getYear();
		long count = missionRepository.countByYear(year);
		entity.setCodeMission(String.format("%d-%03d", year, count + 1));

		// Calcul des champs dérivés
		computeCalculatedFields(entity);

		MissionEntity saved = missionRepository.save(entity);

		// Création automatique de la facture client.
		// Conditions :
		//  - client renseigné
		//  - tarification != INDEFINIE (facturation manuelle plus tard depuis Factures Client)
		//  - flag genererFacture non explicitement à false (défaut = true pour rétro-compat)
		boolean genererFacture = !Boolean.FALSE.equals(request.getGenererFacture());
		if (genererFacture
				&& saved.getClient() != null
				&& saved.getTypeTarification() != TypeTarificationEnum.INDEFINIE) {
			Facture facture = createFactureForMission(saved);

			// Si un compte est fourni, marquer la facture comme payée
			if (request.getCompteId() != null && facture != null) {
				factureService.changerStatut(facture.getId(), FactureStatusEnum.PAYEE, request.getCompteId());
			}
		}

		return missionMapper.toDto(saved);
	}

	@Transactional
	public Mission updateMission(Long id, MissionRequest request) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		// 1. Une mission annulée n'est plus modifiable.
		if (entity.getDhmsAnnulation() != null) {
			throw new BadRequestException("Une mission annulée ne peut pas être modifiée");
		}

		// 2. Validations métier identiques à la création.
		if (Boolean.TRUE.equals(request.getIsSousTraitee())) {
			if (request.getDetailsVehiculeSousTraitance() == null || request.getDetailsVehiculeSousTraitance().isBlank()) {
				throw new BadRequestException("Les détails du véhicule de sous-traitance sont obligatoires pour une mission sous-traitée");
			}
		}

		// 3. Détection des changements "comptables" (impactent la facture).
		boolean comptableChanged = isComptableChanged(entity, request);

		// 4. Si changement comptable + facture liée verrouillée → refus.
		FactureEntity facture = entity.getCodeMission() != null
				? factureRepository.findByLigneExtraRef(entity.getCodeMission()).stream().findFirst().orElse(null)
				: null;

		if (comptableChanged && facture != null && isFactureVerrouillee(facture)) {
			throw new BadRequestException(
					"La facture liée (" + facture.getNumProforma()
							+ ", statut " + facture.getStatut()
							+ (facture.getNature() == FactureNatureEnum.AVOIR ? ", nature AVOIR" : "")
							+ ") interdit toute modification comptable de la mission. "
							+ "Annulez la mission (génère un avoir + remboursement) puis recréez-la.");
		}

		// 5. Application de l'update.
		VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));

		missionMapper.updateEntity(request, entity);
		entity.setVehicule(vehicule);
		resolveRelations(request, entity);

		// 6. Recalcul systématique des champs dérivés (montantTotalHT, totalPerdiem, dureeLocation).
		computeCalculatedFields(entity);

		MissionEntity saved = missionRepository.save(entity);

		// 7. Re-sync facture si elle existe et est encore modifiable (BROUILLON/PROFORMA).
		if (comptableChanged && facture != null && saved.getClient() != null) {
			List<LigneFactureRequest> items = buildMissionFactureItems(saved);
			factureService.replaceMissionFactureLines(facture, saved.getClient(), items);
		}

		return missionMapper.toDto(saved);
	}

	/**
	 * Indique si la requête modifie un champ "comptable" de la mission, càd
	 * un champ qui impacterait les lignes ou le partenaire de la facture liée.
	 */
	private boolean isComptableChanged(MissionEntity entity, MissionRequest request) {
		return entity.getTypeTarification() != request.getTypeTarification()
				|| !Objects.equals(entity.getTarif(), request.getTarif())
				|| !Objects.equals(entity.getPerdiem(), request.getPerdiem())
				|| !Objects.equals(entity.getDhmsDebutPrevi(), request.getDhmsDebutPrevi())
				|| !Objects.equals(entity.getDhmsFinPrevi(), request.getDhmsFinPrevi())
				|| !Objects.equals(entity.getWithChauffeur(), request.getWithChauffeur())
				|| !Objects.equals(entity.getVehicule() != null ? entity.getVehicule().getId() : null, request.getVehiculeId())
				|| !Objects.equals(entity.getClient() != null ? entity.getClient().getId() : null, request.getClientId());
	}

	/**
	 * Une facture est verrouillée pour toute resync mission dès qu'elle a quitté
	 * l'état BROUILLON/PROFORMA, ou si c'est un AVOIR.
	 */
	private boolean isFactureVerrouillee(FactureEntity facture) {
		if (facture.getNature() == FactureNatureEnum.AVOIR) return true;
		FactureStatusEnum s = facture.getStatut();
		return s == FactureStatusEnum.FACTUREE
				|| s == FactureStatusEnum.PAYEE
				|| s == FactureStatusEnum.ANNULEE;
	}

	@Transactional
	public Mission demarrerMission(Long id, LocalDateTime date) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		if (entity.getDhmsAnnulation() != null) {
			throw new BadRequestException("Cette mission est annulée et ne peut pas être démarrée");
		}
		if (entity.getDhmsDebutReel() != null) {
			throw new BadRequestException("Cette mission a déjà été démarrée");
		}

		// Le véhicule doit être DISPONIBLE pour partir en mission (symétrie avec changerVehicule).
		VehiculeEntity vehicule = entity.getVehicule();
		if (vehicule.getStatut() != VehiculeStatusEnum.DISPONIBLE) {
			throw new BadRequestException("Le véhicule " + vehicule.getImmatriculation()
					+ " n'est pas disponible (statut actuel : " + vehicule.getStatut() + ")");
		}

		entity.setDhmsDebutReel(date);
		vehicule.setStatut(VehiculeStatusEnum.MISSION);
		vehiculeRepository.save(vehicule);

		// Passer le chauffeur en statut MISSION s'il est affecté et disponible.
		ChauffeurEntity chauffeur = entity.getChauffeur();
		if (chauffeur != null && Boolean.TRUE.equals(entity.getWithChauffeur())) {
			if (chauffeur.getStatut() != StatutChauffeurEnum.DISPONIBLE) {
				throw new BadRequestException("Le chauffeur " + chauffeur.getEmploye().getNom()
						+ " n'est pas disponible (statut : " + chauffeur.getStatut() + ")");
			}
			chauffeur.setStatut(StatutChauffeurEnum.MISSION);
			chauffeurRepository.save(chauffeur);
		}

		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
	}

	@Transactional
	public Mission terminerMission(Long id, LocalDateTime date) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		if (entity.getDhmsDebutReel() == null) {
			throw new BadRequestException("Cette mission n'a pas encore été démarrée");
		}
		if (entity.getDhmsFinReel() != null) {
			throw new BadRequestException("Cette mission est déjà terminée");
		}

		entity.setDhmsFinReel(date);

		// Restituer le véhicule : DISPONIBLE par défaut, GARAGE s'il a une intervention en cours.
		// Si son statut a été changé en externe (SINISTRE, REFORME, INDISPONIBLE…), on ne l'écrase pas.
		VehiculeEntity vehicule = entity.getVehicule();
		if (vehicule.getStatut() == VehiculeStatusEnum.MISSION) {
			boolean interventionActive = interventionRepository.existsByVehiculeIdAndStatut(
					vehicule.getId(), InterventionStatut.EN_COURS);
			vehicule.setStatut(interventionActive ? VehiculeStatusEnum.GARAGE : VehiculeStatusEnum.DISPONIBLE);
			vehiculeRepository.save(vehicule);
		}

		// Remettre le chauffeur DISPONIBLE s'il était en MISSION.
		ChauffeurEntity chauffeur = entity.getChauffeur();
		if (chauffeur != null && chauffeur.getStatut() == StatutChauffeurEnum.MISSION) {
			chauffeur.setStatut(StatutChauffeurEnum.DISPONIBLE);
			chauffeurRepository.save(chauffeur);
		}

		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
	}

	// ==================== ANNULATION ====================

	/**
	 * Annule une mission non démarrée.
	 * - Si la facture associée est un REÇU : statut → ANNULÉE, et création d'une
	 *   ligne REMBOURSEMENT (débit) sur le compte ayant reçu le paiement.
	 * - Si la facture associée est une PROFORMA : génération d'une facture AVOIR
	 *   liée à l'originale, et statut originale → ANNULÉE. Si la proforma était
	 *   déjà payée, un compte (request.compteId) est requis pour débiter le
	 *   remboursement.
	 */
	@Transactional
	public Mission annulerMission(Long id, AnnulerMissionRequest request) {
		MissionEntity mission = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		if (mission.getDhmsDebutReel() != null) {
			throw new BadRequestException("Cette mission a déjà démarré et ne peut plus être annulée");
		}
		if (mission.getDhmsAnnulation() != null) {
			throw new BadRequestException("Cette mission est déjà annulée");
		}

		FactureEntity facture = mission.getCodeMission() != null
				? factureRepository.findByLigneExtraRef(mission.getCodeMission()).stream().findFirst().orElse(null)
				: null;

		if (facture != null) {
			if (facture.getStatut() == FactureStatusEnum.ANNULEE) {
				throw new BadRequestException("La facture associée est déjà annulée");
			}

			if (facture.getNature() == FactureNatureEnum.RECU) {
				// Cas reçu : retrouver la ligne d'encaissement pour identifier le compte d'origine
				LigneCompteEntity ligneEncaissement = ligneCompteRepository
						.findFirstByFactureIdAndTypeOrderByDhmsOperationAsc(facture.getId(), CompteLigneType.APPROVISIONNEMENT)
						.orElseThrow(() -> new BadRequestException(
								"Impossible de retrouver l'encaissement initial du reçu " + facture.getNumProforma()
										+ ". Veuillez effectuer le remboursement manuellement."));

				Long compteEncaissement = ligneEncaissement.getCompte().getId();
				factureService.enregistrerRemboursement(facture, compteEncaissement);

				facture.setStatut(FactureStatusEnum.ANNULEE);
				factureRepository.save(facture);
			} else if (facture.getNature() == FactureNatureEnum.FACTURE
					|| facture.getNature() == FactureNatureEnum.AVOIR) {
				// Cas proforma / facture client : on génère un avoir
				if (facture.getNature() == FactureNatureEnum.AVOIR) {
					throw new BadRequestException("La facture liée est déjà un avoir");
				}

				boolean dejaPayee = facture.getStatut() == FactureStatusEnum.PAYEE;

				if (dejaPayee && request.getCompteId() == null) {
					throw new BadRequestException(
							"La facture associée a déjà été payée. Le compte de remboursement est obligatoire.");
				}

				// Générer l'avoir
				factureService.genererAvoir(facture);

				if (dejaPayee) {
					factureService.enregistrerRemboursement(facture, request.getCompteId());
				}

				facture.setStatut(FactureStatusEnum.ANNULEE);
				factureRepository.save(facture);
			}
		}

		mission.setDhmsAnnulation(LocalDateTime.now());
		mission.setMotifAnnulation(request.getMotif());
		MissionEntity saved = missionRepository.save(mission);

		return getMissionById(saved.getId());
	}

	// ==================== CHANGEMENT DE VEHICULE ====================

	@Transactional
	public Mission changerVehicule(ChangerVehiculeMissionRequest request) {
		// 1. Récupérer la mission en cours
		MissionEntity ancienneMission = missionRepository.findById(request.getMissionId())
				.orElseThrow(() -> new ResourceNotFoundException("Mission", request.getMissionId()));

		if (ancienneMission.getDhmsDebutReel() == null) {
			throw new BadRequestException("Cette mission n'a pas encore été démarrée");
		}
		if (ancienneMission.getDhmsFinReel() != null) {
			throw new BadRequestException("Cette mission est déjà terminée");
		}

		// 2. Vérifier que le nouveau véhicule est disponible
		VehiculeEntity nouveauVehicule = vehiculeRepository.findById(request.getNouveauVehiculeId())
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getNouveauVehiculeId()));

		if (nouveauVehicule.getStatut() != VehiculeStatusEnum.DISPONIBLE) {
			throw new BadRequestException("Le nouveau véhicule n'est pas disponible (statut actuel : " + nouveauVehicule.getStatut() + ")");
		}

		LocalDateTime maintenant = LocalDateTime.now();
		LocalDateTime finPrevi = ancienneMission.getDhmsFinPrevi();

		// 3. Clôturer l'ancienne mission à la date réelle (maintenant)
		ancienneMission.setDhmsFinReel(maintenant);

		// Recalculer durée et coûts de l'ancienne mission sur la durée réelle.
		// Si la panne survient le 1er jour de mission, on ne facture pas le jour de départ (durée = 0).
		long dureeReelleJours = ChronoUnit.DAYS.between(
				ancienneMission.getDhmsDebutReel().toLocalDate(), maintenant.toLocalDate());
		if (dureeReelleJours < 0) dureeReelleJours = 0;

		TypeTarificationEnum typeAncienne = ancienneMission.getTypeTarification();
		long dureeReelleUnitaire = dureeReelleJours;
		if (typeAncienne == TypeTarificationEnum.MENSUELLE || typeAncienne == TypeTarificationEnum.INDEFINIE) {
			// Pour une tarification mensuelle/indéfinie, la durée facturée est en mois entamés (panne <30j ⇒ 0 mois).
			dureeReelleUnitaire = dureeReelleJours / 30;
		}

		ancienneMission.setDureeLocation(dureeReelleUnitaire);
		if (typeAncienne == TypeTarificationEnum.UNIQUE) {
			// Forfait : montant inchangé, durée enregistrée à titre informatif.
			ancienneMission.setDureeLocation(dureeReelleJours);
		} else if (ancienneMission.getTarif() != null) {
			ancienneMission.setMontantTotalHT(ancienneMission.getTarif().multiply(BigDecimal.valueOf(dureeReelleUnitaire)));
		}
		if (ancienneMission.getPerdiem() != null) {
			ancienneMission.setTotalPerdiem(ancienneMission.getPerdiem().multiply(BigDecimal.valueOf(dureeReelleJours)));
		}

		// Mettre l'ancien véhicule dans le statut choisi par l'utilisateur (GARAGE ou SINISTRE)
		VehiculeEntity ancienVehicule = ancienneMission.getVehicule();
		VehiculeStatusEnum statutAncienVehicule = request.getNouveauStatutAncienVehicule();
		if (statutAncienVehicule != VehiculeStatusEnum.GARAGE && statutAncienVehicule != VehiculeStatusEnum.SINISTRE) {
			statutAncienVehicule = VehiculeStatusEnum.GARAGE; // valeur par défaut sécurisée
		}
		ancienVehicule.setStatut(statutAncienVehicule);
		vehiculeRepository.save(ancienVehicule);

		missionRepository.save(ancienneMission);

		// 4. Créer la nouvelle mission avec la durée restante.
		// Pour une mission INDEFINIE, finPrevi peut être null : pas de durée restante calculable.
		long dureeRestanteJours = (finPrevi != null)
				? Math.max(1, ChronoUnit.DAYS.between(maintenant.toLocalDate(), finPrevi.toLocalDate()))
				: 0;

		// Générer le code mission
		int year = LocalDate.now().getYear();
		long count = missionRepository.countByYear(year);

		MissionEntity nouvelleMission = MissionEntity.builder()
				.reference(ancienneMission.getReference())
				.typeTarification(typeAncienne)
				.codeMission(String.format("%d-%03d", year, count + 1))
				.dhmsDebutPrevi(maintenant)
				.dhmsFinPrevi(finPrevi)
				.dhmsDebutReel(maintenant)
				.dhmsFinReel(null)
				.destination(ancienneMission.getDestination())
				.localisation(ancienneMission.getLocalisation())
				.withChauffeur(ancienneMission.getWithChauffeur())
				.isSousTraitee(ancienneMission.getIsSousTraitee())
				.detailsVehiculeSousTraitance(ancienneMission.getDetailsVehiculeSousTraitance())
				.perdiem(ancienneMission.getPerdiem())
				.tarif(ancienneMission.getTarif())
				.kilometrageDepart(null)
				.kilometrageArrive(null)
				.observations(ancienneMission.getObservations())
				.vehicule(nouveauVehicule)
				.chauffeur(ancienneMission.getChauffeur())
				.client(ancienneMission.getClient())
				.build();

		// Calculer les champs dérivés pour la nouvelle mission (durée restante).
		// - JOURNALIERE : dureeLocation en jours
		// - MENSUELLE : dureeLocation en mois entamés (min 1)
		// - UNIQUE : forfait reporté tel quel (montantTotalHT = tarif)
		// - INDEFINIE : pas de calcul si pas de fin prévi
		if (typeAncienne == TypeTarificationEnum.UNIQUE) {
			// P2 : forfait déjà facturé sur l'ancienne mission → montant = 0 sur la nouvelle
			nouvelleMission.setDureeLocation(dureeRestanteJours);
			nouvelleMission.setMontantTotalHT(BigDecimal.ZERO);
		} else if (typeAncienne == TypeTarificationEnum.INDEFINIE && finPrevi == null) {
			nouvelleMission.setDureeLocation(null);
			nouvelleMission.setMontantTotalHT(null);
		} else {
			long dureeRestanteUnitaire = dureeRestanteJours;
			if (typeAncienne == TypeTarificationEnum.MENSUELLE || typeAncienne == TypeTarificationEnum.INDEFINIE) {
				long nbMois = dureeRestanteJours / 30;
				if (nbMois < 1) nbMois = 1;
				dureeRestanteUnitaire = nbMois;
			}
			nouvelleMission.setDureeLocation(dureeRestanteUnitaire);
			if (nouvelleMission.getTarif() != null) {
				nouvelleMission.setMontantTotalHT(nouvelleMission.getTarif().multiply(BigDecimal.valueOf(dureeRestanteUnitaire)));
			}
		}
		// Perdiem chauffeur : toujours journalier.
		if (nouvelleMission.getPerdiem() != null && dureeRestanteJours > 0) {
			nouvelleMission.setTotalPerdiem(nouvelleMission.getPerdiem().multiply(BigDecimal.valueOf(dureeRestanteJours)));
		}

		// Mettre le nouveau véhicule en MISSION
		nouveauVehicule.setStatut(VehiculeStatusEnum.MISSION);
		vehiculeRepository.save(nouveauVehicule);

		MissionEntity saved = missionRepository.save(nouvelleMission);
		return missionMapper.toDto(saved);
	}

	// ==================== DEPENSES MISSION ====================

	@Transactional
	public DepenseMission addDepense(Long missionId, DepenseMissionRequest request) {
		MissionEntity mission = missionRepository.findById(missionId)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));

		TypeDepenseEntity typeDepense = typeDepenseRepository.findById(request.getTypeDepenseId())
				.orElseThrow(() -> new ResourceNotFoundException("Type de dépense", request.getTypeDepenseId()));

		DepenseMissionEntity entity = depenseMissionMapper.toEntity(request);
		entity.setMission(mission);
		entity.setTypeDepense(typeDepense);

		DepenseMissionEntity saved = depenseMissionRepository.save(entity);

		// Enregistrer la dépense comme ligne de compte
		String objet = "DÉPENSE MISSION " + mission.getCodeMission()
				+ " — " + typeDepense.getLibelle()
				+ (request.getLibelle() != null ? " : " + request.getLibelle() : "");

		LigneCompteRequest ligneRequest = LigneCompteRequest.builder()
				.type(CompteLigneType.DEPENSE)
				.objet(objet)
				.montant(request.getMontant())
				.observation("Mission " + mission.getCodeMission())
				.build();

		compteService.createLigne(request.getCompteId(), ligneRequest);

		return depenseMissionMapper.toDto(saved);
	}

	// ==================== PHOTOS MISSION ====================

	@Transactional
	public PhotoMission addPhoto(Long missionId, MultipartFile file, PhotoMissionTypeEnum type) {
		MissionEntity mission = missionRepository.findById(missionId)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));

		Media media = mediaService.uploadMedia(file);
		MediaEntity mediaEntity = mediaService.getMediaEntity(media.getId());

		PhotoMissionEntity photo = PhotoMissionEntity.builder()
				.mission(mission)
				.media(mediaEntity)
				.type(type)
				.build();
		PhotoMissionEntity saved = photoMissionRepository.save(photo);

		return PhotoMission.builder()
				.id(saved.getId())
				.type(saved.getType())
				.media(media)
				.build();
	}

	// ==================== AFFECTATION CHAUFFEUR ====================

	@Transactional
	public Mission affecterChauffeur(Long id, AffecterChauffeurRequest request) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		if (entity.getDhmsAnnulation() != null) {
			throw new BadRequestException("Une mission annulée ne peut pas être modifiée");
		}
		if (entity.getDhmsDebutReel() != null) {
			throw new BadRequestException("La mission a déjà démarré. Utilisez le changement de véhicule pour modifier l'affectation.");
		}

		if (request.getChauffeurId() != null) {
			ChauffeurEntity chauffeur = chauffeurRepository.findById(request.getChauffeurId())
					.orElseThrow(() -> new ResourceNotFoundException("Chauffeur", request.getChauffeurId()));
			if (chauffeur.getStatut() != StatutChauffeurEnum.DISPONIBLE) {
				throw new BadRequestException("Le chauffeur " + chauffeur.getEmploye().getNom()
						+ " n'est pas disponible (statut : " + chauffeur.getStatut() + ")");
			}
			entity.setChauffeur(chauffeur);
			entity.setWithChauffeur(true);
			entity.setPerdiem(request.getPerdiem());

			// Recalculer le total perdiem si les dates sont connues.
			if (request.getPerdiem() != null && entity.getDhmsDebutPrevi() != null && entity.getDhmsFinPrevi() != null) {
				long nbJours = java.time.temporal.ChronoUnit.DAYS.between(
						entity.getDhmsDebutPrevi().toLocalDate(), entity.getDhmsFinPrevi().toLocalDate());
				if (nbJours < 1) nbJours = 1;
				entity.setTotalPerdiem(request.getPerdiem().multiply(BigDecimal.valueOf(nbJours)));
			} else {
				entity.setTotalPerdiem(null);
			}
		} else {
			entity.setChauffeur(null);
			entity.setWithChauffeur(false);
			entity.setPerdiem(null);
			entity.setTotalPerdiem(null);
		}

		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
	}

	// ==================== SIMULATION ====================

	@Transactional(readOnly = true)
	public SimulationTarif simulerTarif(Long vehiculeId, TypeTarificationEnum typeTarification, LocalDateTime debut, LocalDateTime fin, LocalisationMissionEnum localisation) {
		// Pour les tarifications UNIQUE et INDEFINIE, aucun tarif de référence n'est défini sur le type
		// de véhicule. La simulation est donc neutre (pas de minimum à vérifier côté front).
		if (typeTarification == TypeTarificationEnum.UNIQUE || typeTarification == TypeTarificationEnum.INDEFINIE) {
			return SimulationTarif.builder()
					.duree(0L)
					.tarifMinimum(null)
					.tarifUnitaire(null)
					.typeTarification(typeTarification)
					.build();
		}

		VehiculeEntity vehicule = vehiculeRepository.findById(vehiculeId)
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", vehiculeId));

		TypeVehiculeEntity typeVehicule = vehicule.getType();
		if (typeVehicule == null) {
			throw new BadRequestException("Ce véhicule n'a pas de type défini");
		}

		long dureeJours = ChronoUnit.DAYS.between(debut.toLocalDate(), fin.toLocalDate());
		if (dureeJours < 1) dureeJours = 1;

		long duree;
		BigDecimal tarifMinimum;
		BigDecimal tarifUnitaire;
		if (typeTarification == TypeTarificationEnum.MENSUELLE) {
			BigDecimal prixMensuel = typeVehicule.getPrixMensuel();
			if (prixMensuel == null) prixMensuel = BigDecimal.ZERO;
			long mois = dureeJours / 30;
			if (mois < 1) mois = 1;
			tarifMinimum = prixMensuel.multiply(BigDecimal.valueOf(mois));
			tarifUnitaire = prixMensuel;
			duree = mois;
		} else {
			BigDecimal prixJournalier = typeVehicule.getPrixJournalier();
			if (prixJournalier == null) prixJournalier = BigDecimal.ZERO;
			// Supplément selon localisation, configurable depuis Paramètres > Général (Site).
			BigDecimal supplement = siteService.getSupplementJournalier(localisation);
			BigDecimal prixJournalierMin = prixJournalier.add(supplement);
			tarifMinimum = prixJournalierMin.multiply(BigDecimal.valueOf(dureeJours));
			tarifUnitaire = prixJournalierMin;
			duree = dureeJours;
		}

		return SimulationTarif.builder()
				.duree(duree)
				.tarifMinimum(tarifMinimum)
				.tarifUnitaire(tarifUnitaire)
				.typeTarification(typeTarification)
				.build();
	}

	// ==================== HELPERS ====================

	private void computeCalculatedFields(MissionEntity entity) {
		TypeTarificationEnum type = entity.getTypeTarification();

		// Cas UNIQUE : montant forfaitaire = tarif, indépendant de la durée.
		// La durée en jours reste calculée (à titre informatif) si les dates sont saisies.
		if (type == TypeTarificationEnum.UNIQUE) {
			if (entity.getTarif() != null) {
				entity.setMontantTotalHT(entity.getTarif());
			}
			if (entity.getDhmsDebutPrevi() != null && entity.getDhmsFinPrevi() != null) {
				long nbJours = ChronoUnit.DAYS.between(entity.getDhmsDebutPrevi().toLocalDate(), entity.getDhmsFinPrevi().toLocalDate());
				if (nbJours < 1) nbJours = 1;
				entity.setDureeLocation(nbJours);
				if (entity.getPerdiem() != null) {
					entity.setTotalPerdiem(entity.getPerdiem().multiply(BigDecimal.valueOf(nbJours)));
				}
			}
			return;
		}

		// Cas INDEFINIE : pas de date de fin prévisionnelle ⇒ pas de durée ni de montant calculés à la création.
		// Le tarif renseigné est mensuel ; la facturation se fera manuellement.
		if (type == TypeTarificationEnum.INDEFINIE) {
			if (entity.getDhmsDebutPrevi() != null && entity.getDhmsFinPrevi() != null) {
				long nbJours = ChronoUnit.DAYS.between(entity.getDhmsDebutPrevi().toLocalDate(), entity.getDhmsFinPrevi().toLocalDate());
				if (nbJours < 1) nbJours = 1;
				long nbMois = nbJours / 30;
				if (nbMois < 1) nbMois = 1;
				entity.setDureeLocation(nbMois);
				if (entity.getTarif() != null) {
					entity.setMontantTotalHT(entity.getTarif().multiply(BigDecimal.valueOf(nbMois)));
				}
				if (entity.getPerdiem() != null) {
					entity.setTotalPerdiem(entity.getPerdiem().multiply(BigDecimal.valueOf(nbJours)));
				}
			} else {
				entity.setDureeLocation(null);
				entity.setMontantTotalHT(null);
				entity.setTotalPerdiem(null);
			}
			return;
		}

		// Cas JOURNALIERE / MENSUELLE : montant = tarif unitaire × durée.
		if (entity.getDhmsDebutPrevi() != null && entity.getDhmsFinPrevi() != null) {
			long nbJours = ChronoUnit.DAYS.between(entity.getDhmsDebutPrevi().toLocalDate(), entity.getDhmsFinPrevi().toLocalDate());
			if (nbJours < 1) nbJours = 1;

			// dureeLocation = nombre d'unités de tarification (jours OU mois) — aligné sur simulerTarif.
			long dureeUnitaire = nbJours;
			if (type == TypeTarificationEnum.MENSUELLE) {
				long nbMois = nbJours / 30;
				if (nbMois < 1) nbMois = 1;
				dureeUnitaire = nbMois;
			}
			entity.setDureeLocation(dureeUnitaire);

			if (entity.getTarif() != null) {
				entity.setMontantTotalHT(entity.getTarif().multiply(BigDecimal.valueOf(dureeUnitaire)));
			}

			// Le perdiem reste toujours journalier, quelle que soit la tarification du véhicule.
			if (entity.getPerdiem() != null) {
				entity.setTotalPerdiem(entity.getPerdiem().multiply(BigDecimal.valueOf(nbJours)));
			}
		}

		// Filet de sécurité : durée minimale = 1 (quelle que soit la présence des dates).
		if (entity.getDureeLocation() == null || entity.getDureeLocation() < 1) {
			entity.setDureeLocation(1L);
			if (entity.getTarif() != null) {
				entity.setMontantTotalHT(entity.getTarif());
			}
		}
	}

	private Facture createFactureForMission(MissionEntity mission) {
		List<LigneFactureRequest> items = buildMissionFactureItems(mission);
		long totalHt = items.stream().mapToLong(i -> i.getMontantHt() != null ? i.getMontantHt() : 0).sum();

		// La numérotation client (DA/01/79/{seq}) est gérée par FactureService.createFacture.
		// Le lien mission ↔ facture passe par LigneFacture.extraRef = mission.codeMission.
		FactureRequest factureRequest = FactureRequest.builder()
				.objet("MISSION " + mission.getCodeMission())
				.factureClient(true)
				.partenaireId(mission.getClient().getId())
				.tva(0f)
				.montantHt(totalHt)
				.montantTtc(totalHt)
				.items(items)
				.build();

		return factureService.createFacture(factureRequest, FactureTypeEnum.MISSION);
	}

	/**
	 * Construit les lignes de facture d'une mission (location + ligne perdiem
	 * chauffeur si applicable). Utilisé à la création et à la resynchronisation
	 * d'une facture en BROUILLON/PROFORMA après update de la mission.
	 */
	private List<LigneFactureRequest> buildMissionFactureItems(MissionEntity mission) {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		boolean hasDates = mission.getDhmsDebutPrevi() != null && mission.getDhmsFinPrevi() != null;
		String dateDebut = hasDates ? mission.getDhmsDebutPrevi().format(fmt) : "";
		String dateFin = hasDates ? mission.getDhmsFinPrevi().format(fmt) : "";

		TypeTarificationEnum type = mission.getTypeTarification();
		boolean unique = type == TypeTarificationEnum.UNIQUE;
		boolean mensuelle = type == TypeTarificationEnum.MENSUELLE || type == TypeTarificationEnum.INDEFINIE;
		String uniteLabel = mensuelle ? " mois" : " jour(s)";
		String duree = mission.getDureeLocation() != null ? (mission.getDureeLocation() + uniteLabel) : "";

		// Nombre de jours réels (toujours), utilisé pour la ligne perdiem chauffeur.
		long nbJours = hasDates
				? Math.max(1, ChronoUnit.DAYS.between(mission.getDhmsDebutPrevi().toLocalDate(), mission.getDhmsFinPrevi().toLocalDate()))
				: 0;

		String immat = mission.getVehicule() != null ? mission.getVehicule().getImmatriculation() : "";

		List<LigneFactureRequest> items = new ArrayList<>();

		long montantLocationHt = mission.getMontantTotalHT() != null ? mission.getMontantTotalHT().longValue() : 0;
		String designation;
		if (unique) {
			designation = "MISSION " + mission.getCodeMission() + " - " + immat + " (forfait)"
					+ (hasDates ? " du " + dateDebut + " au " + dateFin : "");
		} else {
			designation = "MISSION " + mission.getCodeMission() + " - " + immat
					+ (duree.isEmpty() ? "" : " de " + duree)
					+ (hasDates ? " du " + dateDebut + " au " + dateFin : "");
		}
		items.add(LigneFactureRequest.builder()
				.designation(designation)
				.qte(1L)
				.prixUnitaire(montantLocationHt)
				.remise(0f)
				.montantHt(montantLocationHt)
				.extraRef(mission.getCodeMission())
				.build());

		if (Boolean.TRUE.equals(mission.getWithChauffeur()) && mission.getPerdiem() != null && mission.getPerdiem().compareTo(BigDecimal.ZERO) > 0 && nbJours > 0) {
			long perdiemUnitaire = mission.getPerdiem().longValue();
			long totalPerdiem = mission.getTotalPerdiem() != null ? mission.getTotalPerdiem().longValue() : perdiemUnitaire * nbJours;
			items.add(LigneFactureRequest.builder()
					.designation("Chauffeur - Perdiem " + immat + " " + nbJours + " jour(s)")
					.qte(nbJours)
					.prixUnitaire(perdiemUnitaire)
					.remise(0f)
					.montantHt(totalPerdiem)
					.extraRef(mission.getCodeMission())
					.build());
		}

		return items;
	}

	private void resolveRelations(MissionRequest request, MissionEntity entity) {
		if (Boolean.TRUE.equals(request.getWithChauffeur()) && request.getChauffeurId() != null) {
			ChauffeurEntity chauffeur = chauffeurRepository.findById(request.getChauffeurId())
					.orElseThrow(() -> new ResourceNotFoundException("Chauffeur", request.getChauffeurId()));
			entity.setChauffeur(chauffeur);
		} else {
			entity.setChauffeur(null);
		}

		if (request.getClientId() != null) {
			PartenaireEntity client = partenaireRepository.findById(request.getClientId())
					.orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getClientId()));
			entity.setClient(client);
		} else {
			entity.setClient(null);
		}
	}
}
