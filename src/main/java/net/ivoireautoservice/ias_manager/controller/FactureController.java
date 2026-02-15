package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.core.LigneFacture;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneFactureRequest;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.services.FactureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureController {

	private final FactureService factureService;
	private final Logger logger = LoggerFactory.getLogger(FactureController.class);

	// ==================== FACTURES ====================

	@GetMapping
	public ResponseEntity<PagedResponse<Facture>> getAllFactures(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "asc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(factureService.getAllFactures(pageable));
	}

	@GetMapping("/clients")
	public ResponseEntity<PagedResponse<Facture>> getFacturesClients(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "asc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(factureService.getFacturesClients(pageable));
	}

	@GetMapping("/fournisseurs")
	public ResponseEntity<PagedResponse<Facture>> getFacturesFournisseurs(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "asc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(factureService.getFacturesFournisseurs(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Facture> getFactureById(@PathVariable Long id) {
		return ResponseEntity.ok(factureService.getFactureById(id));
	}

	@GetMapping("/proforma/{numProforma}")
	public ResponseEntity<Facture> getFactureByNumProforma(@PathVariable String numProforma) {
		return ResponseEntity.ok(factureService.getFactureByNumProforma(numProforma));
	}

	@GetMapping("/numero/{numFacture}")
	public ResponseEntity<Facture> getFactureByNumFacture(@PathVariable String numFacture) {
		return ResponseEntity.ok(factureService.getFactureByNumFacture(numFacture));
	}

	@PostMapping
	public ResponseEntity<Facture> createFacture(@Valid @RequestBody FactureRequest request) {
		Facture created = factureService.createFacture(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Facture> updateFacture(
			@PathVariable Long id,
			@Valid @RequestBody FactureRequest request) {
		return ResponseEntity.ok(factureService.updateFacture(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFacture(@PathVariable Long id) {
		factureService.deleteFacture(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/statut")
	public ResponseEntity<?> changerStatut(
			@PathVariable Long id,
			@RequestParam FactureStatusEnum statut) {
		try{
			return ResponseEntity.ok(factureService.changerStatut(id, statut));
		}catch (Exception e){
			logger.error("ERREUR | {} | {}", e.getMessage(), e.getStackTrace());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}