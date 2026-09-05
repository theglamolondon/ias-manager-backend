package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.FactureFournisseurGroupeeRequest;
import net.ivoireautoservice.ias_manager.dto.request.FactureMissionGroupeeRequest;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.services.FactureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('FACTURE_CLIENT_READ','FACTURE_FOURNISSEUR_READ')")
public class FactureController {

	private final FactureService factureService;
	private final Logger logger = LoggerFactory.getLogger(FactureController.class);

	// ==================== FACTURES ====================

	@GetMapping
	public ResponseEntity<PagedResponse<Facture>> getAllFactures(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Boolean factureClient,
			@RequestParam(required = false) Long partenaireId,
			@RequestParam(required = false) String numFacture,
			@RequestParam(required = false) String codeMission,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "createdAt") String tri,
			@RequestParam(defaultValue = "desc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(factureService.getAllFactures(keyword, factureClient, partenaireId,
				numFacture, codeMission, dateDebut, dateFin, pageable));
	}

	@GetMapping("/clients")
	@PreAuthorize("hasAuthority('FACTURE_CLIENT_READ')")
	public ResponseEntity<PagedResponse<Facture>> getFacturesClients(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long partenaireId,
			@RequestParam(required = false) String numFacture,
			@RequestParam(required = false) String codeMission,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "createdAt") String tri,
			@RequestParam(defaultValue = "desc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(factureService.getFacturesClients(keyword, partenaireId,
				numFacture, codeMission, dateDebut, dateFin, pageable));
	}

	@GetMapping("/fournisseurs")
	@PreAuthorize("hasAuthority('FACTURE_FOURNISSEUR_READ')")
	public ResponseEntity<PagedResponse<Facture>> getFacturesFournisseurs(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long partenaireId,
			@RequestParam(required = false) String numFacture,
			@RequestParam(required = false) String codeMission,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "createdAt") String tri,
			@RequestParam(defaultValue = "desc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(factureService.getFacturesFournisseurs(keyword, partenaireId,
				numFacture, codeMission, dateDebut, dateFin, pageable));
	}

	@GetMapping("/livrables")
	public ResponseEntity<PagedResponse<Facture>> getFacturesLivrables(
			@RequestParam(required = false) Boolean factureClient,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille) {
		Pageable pageable = PageRequest.of(page, taille, Sort.by("createdAt").descending());
		return ResponseEntity.ok(factureService.getFacturesLivrables(factureClient, pageable));
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
	@PreAuthorize("hasAnyAuthority('FACTURE_CLIENT_CREATE','FACTURE_FOURNISSEUR_CREATE')")
	public ResponseEntity<Facture> createFacture(@Valid @RequestBody FactureRequest request) {
		Facture created = factureService.createFacture(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/fournisseurs/groupee")
	@PreAuthorize("hasAuthority('FACTURE_FOURNISSEUR_CREATE')")
	public ResponseEntity<Facture> genererFactureFournisseurGroupee(
			@Valid @RequestBody FactureFournisseurGroupeeRequest request) {
		Facture created = factureService.genererFactureFournisseurGroupee(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/missions/groupee")
	@PreAuthorize("hasAuthority('FACTURE_CLIENT_CREATE')")
	public ResponseEntity<Facture> genererFactureMissionGroupee(
			@Valid @RequestBody FactureMissionGroupeeRequest request) {
		Facture created = factureService.genererFactureMissionGroupee(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('FACTURE_CLIENT_CREATE','FACTURE_FOURNISSEUR_CREATE')")
	public ResponseEntity<Facture> updateFacture(
			@PathVariable Long id,
			@Valid @RequestBody FactureRequest request) {
		return ResponseEntity.ok(factureService.updateFacture(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('FACTURE_CLIENT_DELETE','FACTURE_FOURNISSEUR_DELETE')")
	public ResponseEntity<Void> deleteFacture(@PathVariable Long id) {
		factureService.deleteFacture(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/pdf")
	public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
		byte[] pdf = factureService.generatePdf(id);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "facture-" + id + ".pdf");
		return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
	}

	@GetMapping("/{id}/recu")
	public ResponseEntity<byte[]> generateRecu(@PathVariable Long id) {
		byte[] pdf = factureService.generatePieceDeConsigne(id);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "recu-" + id + ".pdf");
		return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
	}

	@PatchMapping("/{id}/statut")
	@PreAuthorize("hasAnyAuthority('FACTURE_CLIENT_VALIDER','FACTURE_FOURNISSEUR_VALIDER')")
	public ResponseEntity<?> changerStatut(
			@PathVariable Long id,
			@RequestParam FactureStatusEnum statut,
			@RequestParam(required = false) Long compteId) {
		try{
			return ResponseEntity.ok(factureService.changerStatut(id, statut, compteId));
		}catch (Exception e){
			logger.error("ERREUR | {} | {}", e.getMessage(), e.getStackTrace());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}