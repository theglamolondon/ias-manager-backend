package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.DepenseMission;
import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.dto.core.Mission;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.services.MissionService;
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
