package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.enums.MissionStatutFilter;
import net.ivoireautoservice.ias_manager.enums.PhotoMissionTypeEnum;
import net.ivoireautoservice.ias_manager.enums.LocalisationMissionEnum;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;
import net.ivoireautoservice.ias_manager.dto.request.AffecterChauffeurRequest;
import net.ivoireautoservice.ias_manager.dto.request.AnnulerMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.ChangerVehiculeMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.services.MissionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MISSION_READ')")
public class MissionController {

	private final MissionService missionService;

	@GetMapping
	public ResponseEntity<PagedResponse<Mission>> getAllMissions(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) MissionStatutFilter statut,
			@RequestParam(required = false) Long partenaireId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "desc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(missionService.getAllMissions(keyword, statut, partenaireId, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Mission> getMissionById(@PathVariable Long id) {
		return ResponseEntity.ok(missionService.getMissionById(id));
	}

	@GetMapping("/facturables")
	public ResponseEntity<List<Mission>> getMissionsFacturables(@RequestParam Long clientId) {
		return ResponseEntity.ok(missionService.getMissionsFacturables(clientId));
	}

	@GetMapping("/chauffeur/{chauffeurId}")
	public ResponseEntity<List<Mission>> getMissionsByChauffeur(@PathVariable Long chauffeurId) {
		return ResponseEntity.ok(missionService.getMissionsByChauffeur(chauffeurId));
	}

	@GetMapping("/{id}/ordre-mission")
	public ResponseEntity<byte[]> generateOrdreMission(@PathVariable Long id) {
		byte[] pdf = missionService.generateOrdreMissionPdf(id);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "ordre-mission-" + id + ".pdf");
		return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
	}

	@GetMapping("/{id}/factures")
	public ResponseEntity<List<Facture>> getFacturesByMission(@PathVariable Long id) {
		return ResponseEntity.ok(missionService.getFacturesByMissionId(id));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('MISSION_CREATE')")
	public ResponseEntity<Mission> createMission(@Valid @RequestBody MissionRequest request) {
		Mission created = missionService.createMission(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> updateMission(
			@PathVariable Long id,
			@Valid @RequestBody MissionRequest request) {
		return ResponseEntity.ok(missionService.updateMission(id, request));
	}

	@PatchMapping("/{id}/demarrer")
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> demarrerMission(
			@PathVariable Long id,
			@RequestParam LocalDateTime date) {
		return ResponseEntity.ok(missionService.demarrerMission(id, date));
	}

	@PatchMapping("/{id}/terminer")
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> terminerMission(
			@PathVariable Long id,
			@RequestParam LocalDateTime date) {
		return ResponseEntity.ok(missionService.terminerMission(id, date));
	}

	// ==================== DEMARRAGE / CLOTURE AVEC MEDIAS (ATOMIQUE) ====================

	@PostMapping(value = "/{id}/demarrer-complet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> demarrerMissionComplet(
			@PathVariable Long id,
			@RequestParam LocalDateTime date,
			@RequestParam(required = false) Long chauffeurId,
			@RequestParam(required = false) BigDecimal perdiem,
			@RequestParam(required = false) List<MultipartFile> files,
			@RequestParam(required = false) List<PhotoMissionTypeEnum> types) {
		return ResponseEntity.ok(missionService.demarrerMissionAvecMedias(id, date, chauffeurId, perdiem, files, types));
	}

	@PostMapping(value = "/{id}/terminer-complet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> terminerMissionComplet(
			@PathVariable Long id,
			@RequestParam LocalDateTime date,
			@RequestParam(required = false) List<MultipartFile> files,
			@RequestParam(required = false) List<PhotoMissionTypeEnum> types) {
		return ResponseEntity.ok(missionService.terminerMissionAvecMedias(id, date, files, types));
	}

	@PostMapping("/{id}/annuler")
	@PreAuthorize("hasAuthority('MISSION_ANNULER')")
	public ResponseEntity<Mission> annulerMission(
			@PathVariable Long id,
			@RequestBody(required = false) AnnulerMissionRequest request) {
		return ResponseEntity.ok(missionService.annulerMission(id, request != null ? request : new AnnulerMissionRequest()));
	}

	// ==================== AFFECTATION CHAUFFEUR ====================

	@PatchMapping("/{id}/affecter-chauffeur")
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> affecterChauffeur(
			@PathVariable Long id,
			@RequestBody AffecterChauffeurRequest request) {
		return ResponseEntity.ok(missionService.affecterChauffeur(id, request));
	}

	// ==================== CHANGEMENT DE VEHICULE ====================

	@PatchMapping("/changer-vehicule")
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<Mission> changerVehicule(
			@Valid @RequestBody ChangerVehiculeMissionRequest request) {
		Mission nouvelleMission = missionService.changerVehicule(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleMission);
	}

	// ==================== SIMULATION ====================

	@GetMapping("/simulation")
	public ResponseEntity<SimulationTarif> simulerTarif(
			@RequestParam Long vehiculeId,
			@RequestParam TypeTarificationEnum typeTarification,
			@RequestParam LocalDateTime debut,
			@RequestParam LocalDateTime fin,
			@RequestParam(required = false) LocalisationMissionEnum localisation) {
		return ResponseEntity.ok(missionService.simulerTarif(vehiculeId, typeTarification, debut, fin, localisation));
	}

	// ==================== DEPENSES ====================

	@PostMapping("/{missionId}/depenses")
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<DepenseMission> addDepense(
			@PathVariable Long missionId,
			@Valid @RequestBody DepenseMissionRequest request) {
		DepenseMission created = missionService.addDepense(missionId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// ==================== PHOTOS ====================

	@PostMapping(value = "/{missionId}/medias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('MISSION_UPDATE')")
	public ResponseEntity<PhotoMission> addPhoto(
			@PathVariable Long missionId,
			@RequestParam MultipartFile file,
			@RequestParam(required = false) PhotoMissionTypeEnum type) {
		PhotoMission photo = missionService.addPhoto(missionId, file, type);
		return ResponseEntity.status(HttpStatus.CREATED).body(photo);
	}
}
