package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;
import net.ivoireautoservice.ias_manager.dto.request.ChangerVehiculeMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.services.MissionService;
import java.time.LocalDateTime;
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
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "desc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(missionService.getAllMissions(keyword, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Mission> getMissionById(@PathVariable Long id) {
		return ResponseEntity.ok(missionService.getMissionById(id));
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
			@RequestParam LocalDateTime fin) {
		return ResponseEntity.ok(missionService.simulerTarif(vehiculeId, typeTarification, debut, fin));
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
