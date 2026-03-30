package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.ChangerVehiculeMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneFactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureTypeEnum;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.DepenseMissionMapper;
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
	private final MissionMapper missionMapper;
	private final DepenseMissionMapper depenseMissionMapper;
	private final MediaMapper mediaMapper;
	private final MediaService mediaService;
	private final FactureService factureService;
	private final CompteService compteService;

	// ==================== MISSIONS ====================

	@Transactional(readOnly = true)
	public PagedResponse<Mission> getAllMissions(String keyword, Pageable pageable) {
		Page<MissionEntity> page = (keyword != null && !keyword.isBlank())
				? missionRepository.searchByKeyword(keyword.trim(), pageable)
				: missionRepository.findAll(pageable);
		return PagedResponse.of(page.map(missionMapper::toDto));
	}

	@Transactional(readOnly = true)
	public Mission getMissionById(Long id) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));
		Mission dto = missionMapper.toDto(entity);

		List<DepenseMissionEntity> depenses = depenseMissionRepository.findByMissionId(id);
		dto.setDepenses(depenseMissionMapper.toDtoList(depenses));

		List<PhotoMissionEntity> photos = photoMissionRepository.findByMissionId(id);
		List<Media> medias = photos.stream()
				.map(photo -> mediaMapper.toDto(photo.getMedia()))
				.toList();
		dto.setMedias(medias);

		return dto;
	}

	@Transactional
	public Mission createMission(MissionRequest request) {
		// Validation sous-traitance / chauffeur
		if (Boolean.TRUE.equals(request.getIsSousTraitee())) {
			if (request.getDetailsVehiculeSousTraitance() == null || request.getDetailsVehiculeSousTraitance().isBlank()) {
				throw new BadRequestException("Les détails du véhicule de sous-traitance sont obligatoires pour une mission sous-traitée");
			}
		} else {
			if (Boolean.TRUE.equals(request.getWithChauffeur()) && request.getChauffeurId() == null) {
				throw new BadRequestException("Le chauffeur est obligatoire pour une mission avec chauffeur");
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

		// Création automatique de la facture client
		if (saved.getClient() != null) {
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

		VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));

		missionMapper.updateEntity(request, entity);
		entity.setVehicule(vehicule);
		resolveRelations(request, entity);

		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
	}

	@Transactional
	public Mission demarrerMission(Long id, LocalDateTime date) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		if (entity.getDhmsDebutReel() != null) {
			throw new BadRequestException("Cette mission a déjà été démarrée");
		}

		entity.setDhmsDebutReel(date);
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
		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
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

		// Recalculer durée et coûts de l'ancienne mission sur la durée réelle
		long dureeReelle = ChronoUnit.DAYS.between(
				ancienneMission.getDhmsDebutReel().toLocalDate(), maintenant.toLocalDate());
		if (dureeReelle < 1) dureeReelle = 1;

		ancienneMission.setDureeLocation(dureeReelle);
		if (ancienneMission.getTarif() != null) {
			ancienneMission.setMontantTotalHT(ancienneMission.getTarif().multiply(BigDecimal.valueOf(dureeReelle)));
		}
		if (ancienneMission.getPerdiem() != null) {
			ancienneMission.setTotalPerdiem(ancienneMission.getPerdiem().multiply(BigDecimal.valueOf(dureeReelle)));
		}

		// Libérer l'ancien véhicule
		VehiculeEntity ancienVehicule = ancienneMission.getVehicule();
		ancienVehicule.setStatut(VehiculeStatusEnum.DISPONIBLE);
		vehiculeRepository.save(ancienVehicule);

		missionRepository.save(ancienneMission);

		// 4. Créer la nouvelle mission avec la durée restante
		long dureeRestante = ChronoUnit.DAYS.between(maintenant.toLocalDate(), finPrevi.toLocalDate());
		if (dureeRestante < 1) dureeRestante = 1;

		// Générer le code mission
		int year = LocalDate.now().getYear();
		long count = missionRepository.countByYear(year);

		MissionEntity nouvelleMission = MissionEntity.builder()
				.reference(ancienneMission.getReference())
				.typeTarification(ancienneMission.getTypeTarification())
				.codeMission(String.format("%d-%03d", year, count + 1))
				.dhmsDebutPrevi(maintenant)
				.dhmsFinPrevi(finPrevi)
				.dhmsDebutReel(maintenant)
				.dhmsFinReel(null)
				.destination(ancienneMission.getDestination())
				.isInterieur(ancienneMission.getIsInterieur())
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

		// Calculer les champs dérivés pour la nouvelle mission (durée restante)
		nouvelleMission.setDureeLocation(dureeRestante);
		if (nouvelleMission.getTarif() != null) {
			nouvelleMission.setMontantTotalHT(nouvelleMission.getTarif().multiply(BigDecimal.valueOf(dureeRestante)));
		}
		if (nouvelleMission.getPerdiem() != null) {
			nouvelleMission.setTotalPerdiem(nouvelleMission.getPerdiem().multiply(BigDecimal.valueOf(dureeRestante)));
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
	public Media addPhoto(Long missionId, MultipartFile file) {
		MissionEntity mission = missionRepository.findById(missionId)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));

		Media media = mediaService.uploadMedia(file);
		MediaEntity mediaEntity = mediaService.getMediaEntity(media.getId());

		PhotoMissionEntity photo = PhotoMissionEntity.builder()
				.mission(mission)
				.media(mediaEntity)
				.build();
		photoMissionRepository.save(photo);

		return media;
	}

	// ==================== SIMULATION ====================

	@Transactional(readOnly = true)
	public SimulationTarif simulerTarif(Long vehiculeId, TypeTarificationEnum typeTarification, LocalDateTime debut, LocalDateTime fin) {
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
		if (typeTarification == TypeTarificationEnum.MENSUELLE) {
			BigDecimal prixMensuel = typeVehicule.getPrixMensuel();
			if (prixMensuel == null) prixMensuel = BigDecimal.ZERO;
			long mois = dureeJours / 30;
			if (mois < 1) mois = 1;
			tarifMinimum = prixMensuel.multiply(BigDecimal.valueOf(mois));
			duree = mois;
		} else {
			BigDecimal prixJournalier = typeVehicule.getPrixJournalier();
			if (prixJournalier == null) prixJournalier = BigDecimal.ZERO;
			tarifMinimum = prixJournalier.multiply(BigDecimal.valueOf(dureeJours));
			duree = dureeJours;
		}

		BigDecimal tarifUnitaire = typeTarification == TypeTarificationEnum.MENSUELLE
				? (typeVehicule.getPrixMensuel() != null ? typeVehicule.getPrixMensuel() : BigDecimal.ZERO)
				: (typeVehicule.getPrixJournalier() != null ? typeVehicule.getPrixJournalier() : BigDecimal.ZERO);

		return SimulationTarif.builder()
				.duree(duree)
				.tarifMinimum(tarifMinimum)
				.tarifUnitaire(tarifUnitaire)
				.typeTarification(typeTarification)
				.build();
	}

	// ==================== HELPERS ====================

	private void computeCalculatedFields(MissionEntity entity) {
		if (entity.getDhmsDebutPrevi() != null && entity.getDhmsFinPrevi() != null) {
			long nbreJours = ChronoUnit.DAYS.between(entity.getDhmsDebutPrevi().toLocalDate(), entity.getDhmsFinPrevi().toLocalDate());
			if (nbreJours < 1) nbreJours = 1;
			entity.setDureeLocation(nbreJours);

			if (entity.getTarif() != null) {
				entity.setMontantTotalHT(entity.getTarif().multiply(BigDecimal.valueOf(nbreJours)));
			}

			if (entity.getPerdiem() != null) {
				entity.setTotalPerdiem(entity.getPerdiem().multiply(BigDecimal.valueOf(nbreJours)));
			}
		}
	}

	private Facture createFactureForMission(MissionEntity mission) {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String dateDebut = mission.getDhmsDebutPrevi().format(fmt);
		String dateFin = mission.getDhmsFinPrevi().format(fmt);
		String duree = mission.getDureeLocation() + " jour(s)";
		String immat = mission.getVehicule() != null ? mission.getVehicule().getImmatriculation() : "";

		List<LigneFactureRequest> items = new ArrayList<>();

		// Ligne principale : location véhicule
		long montantLocationHt = mission.getMontantTotalHT() != null ? mission.getMontantTotalHT().longValue() : 0;
		items.add(LigneFactureRequest.builder()
				.designation("MISSION " + mission.getCodeMission() + " - " + immat + " de " + duree + " du " + dateDebut + " au " + dateFin)
				.qte(1L)
				.prixUnitaire(montantLocationHt)
				.remise(0f)
				.montantHt(montantLocationHt)
				.extraRef(mission.getCodeMission())
				.build());

		// Ligne chauffeur si option sélectionnée
		if (Boolean.TRUE.equals(mission.getWithChauffeur()) && mission.getPerdiem() != null && mission.getPerdiem().compareTo(BigDecimal.ZERO) > 0) {
			long perdiemUnitaire = mission.getPerdiem().longValue();
			long totalPerdiem = mission.getTotalPerdiem() != null ? mission.getTotalPerdiem().longValue() : perdiemUnitaire * mission.getDureeLocation();
			items.add(LigneFactureRequest.builder()
					.designation("Chauffeur - Perdiem " + immat + " " + duree)
					.qte(mission.getDureeLocation())
					.prixUnitaire(perdiemUnitaire)
					.remise(0f)
					.montantHt(totalPerdiem)
					.extraRef(mission.getCodeMission())
					.build());
		}

		long totalHt = items.stream().mapToLong(i -> i.getMontantHt() != null ? i.getMontantHt() : 0).sum();

		FactureRequest factureRequest = FactureRequest.builder()
				.numProforma(mission.getCodeMission())
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
