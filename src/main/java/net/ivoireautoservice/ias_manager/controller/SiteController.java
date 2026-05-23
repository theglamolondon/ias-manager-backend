package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Site;
import net.ivoireautoservice.ias_manager.dto.request.SiteRequest;
import net.ivoireautoservice.ias_manager.services.SiteService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

	private final SiteService siteService;

	@GetMapping
	public ResponseEntity<PagedResponse<Site>> getAllSites(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "asc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(siteService.getAllSites(pageable));
	}

	@GetMapping("/current")
	public ResponseEntity<Site> getCurrentSite() {
		return ResponseEntity.ok(siteService.getCurrentSite());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Site> getSiteById(@PathVariable Long id) {
		return ResponseEntity.ok(siteService.getSiteById(id));
	}

	@PostMapping
	public ResponseEntity<Site> createSite(@Valid @RequestBody SiteRequest request) {
		Site created = siteService.createSite(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Site> updateSite(
			@PathVariable Long id,
			@Valid @RequestBody SiteRequest request) {
		return ResponseEntity.ok(siteService.updateSite(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
		siteService.deleteSite(id);
		return ResponseEntity.noContent().build();
	}
}
