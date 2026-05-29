package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.enums.MissionStatutFilter;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;
import net.ivoireautoservice.ias_manager.dto.request.AffecterChauffeurRequest;
import net.ivoireautoservice.ias_manager.dto.request.AnnulerMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.ChangerVehiculeMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.services.MissionService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
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

	@GetMapping("/{id}/factures")
	public ResponseEntity<List<Facture>> getFacturesByMission(@PathVariable Long id) {
		return ResponseEntity.ok(missionService.getFacturesByMissionId(id));
	}

	@PostMapping
	public ResponseEntity<Mission> createMission(@Valid @RequestBody MissionRequest request) {
		Mission created = missionService.createMission(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Mission> updateMission(
			@PathVariable Long id,
			@Valid @RequestBody MissionRequest request) {
		return ResponseEntity.ok(missionService.updateMission(id, request));
	}

	@PatchMapping("/{id}/demarrer")
	public ResponseEntity<Mission> demarrerMission(
			@PathVariable Long id,
			@RequestParam LocalDateTime date) {
		return ResponseEntity.ok(missionService.demarrerMission(id, date));
	}

	@PatchMapping("/{id}/terminer")
	public ResponseEntity<Mission> terminerMission(
			@PathVariable Long id,
			@RequestParam LocalDateTime date) {
		return ResponseEntity.ok(missionService.terminerMission(id, date));
	}

	@PostMapping("/{id}/annuler")
	public ResponseEntity<Mission> annulerMission(
			@PathVariable Long id,
			@RequestBody(required = false) AnnulerMissionRequest request) {
		return ResponseEntity.ok(missionService.annulerMission(id, request != null ? request : new AnnulerMissionRequest()));
	}

	// ==================== AFFECTATION CHAUFFEUR ====================

	@PatchMapping("/{id}/affecter-chauffeur")
	public ResponseEntity<Mission> affecterChauffeur(
			@PathVariable Long id,
			@RequestBody AffecterChauffeurRequest request) {
		return ResponseEntity.ok(missionService.affecterChauffeur(id, request));
	}

	// ==================== CHANGEMENT DE VEHICULE ====================

	@PatchMapping("/changer-vehicule")
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
			@RequestParam(required = false) Boolean isInterieur) {
		return ResponseEntity.ok(missionService.simulerTarif(vehiculeId, typeTarification, debut, fin, isInterieur));
	}

	// ==================== DEPENSES ====================

	@PostMapping("/{missionId}/depenses")
	public ResponseEntity<DepenseMission> addDepense(
			@PathVariable Long missionId,
			@Valid @RequestBody DepenseMissionRequest request) {
		DepenseMission created = missionService.addDepense(missionId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// ==================== PHOTOS ====================

	@PostMapping(value = "/{missionId}/medias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Media> addPhoto(
			@PathVariable Long missionId,
			@RequestParam MultipartFile file) {
		Media media = missionService.addPhoto(missionId, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(media);
	}
}
