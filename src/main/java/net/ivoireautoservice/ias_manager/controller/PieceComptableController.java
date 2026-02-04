package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.LignePieceComptable;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.PieceComptable;
import net.ivoireautoservice.ias_manager.dto.request.LignePieceComptableRequest;
import net.ivoireautoservice.ias_manager.dto.request.PieceComptableRequest;
import net.ivoireautoservice.ias_manager.services.PieceComptableService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pieces-comptables")
@RequiredArgsConstructor
public class PieceComptableController {

	private final PieceComptableService pieceComptableService;

	// ==================== PIECES COMPTABLES ====================

	@GetMapping
	public ResponseEntity<PagedResponse<PieceComptable>> getAllPiecesComptables(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "asc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(pieceComptableService.getAllPiecesComptables(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PieceComptable> getPieceComptableById(@PathVariable Long id) {
		return ResponseEntity.ok(pieceComptableService.getPieceComptableById(id));
	}

	@GetMapping("/proforma/{numProforma}")
	public ResponseEntity<PieceComptable> getPieceComptableByNumProforma(@PathVariable String numProforma) {
		return ResponseEntity.ok(pieceComptableService.getPieceComptableByNumProforma(numProforma));
	}

	@GetMapping("/facture/{numFacture}")
	public ResponseEntity<PieceComptable> getPieceComptableByNumFacture(@PathVariable String numFacture) {
		return ResponseEntity.ok(pieceComptableService.getPieceComptableByNumFacture(numFacture));
	}

	@PostMapping
	public ResponseEntity<PieceComptable> createPieceComptable(@Valid @RequestBody PieceComptableRequest request) {
		PieceComptable created = pieceComptableService.createPieceComptable(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<PieceComptable> updatePieceComptable(
			@PathVariable Long id,
			@Valid @RequestBody PieceComptableRequest request) {
		return ResponseEntity.ok(pieceComptableService.updatePieceComptable(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePieceComptable(@PathVariable Long id) {
		pieceComptableService.deletePieceComptable(id);
		return ResponseEntity.noContent().build();
	}

	// ==================== LIGNES PIECE COMPTABLE ====================

	@GetMapping("/{pieceId}/lignes")
	public ResponseEntity<PagedResponse<LignePieceComptable>> getLignesByPieceComptable(
			@PathVariable Long pieceId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille,
			@RequestParam(defaultValue = "id") String tri,
			@RequestParam(defaultValue = "asc") String ordre) {
		Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
		Pageable pageable = PageRequest.of(page, taille, sort);
		return ResponseEntity.ok(pieceComptableService.getLignesByPieceComptable(pieceId, pageable));
	}

	@GetMapping("/{pieceId}/lignes/{ligneId}")
	public ResponseEntity<LignePieceComptable> getLigneById(
			@PathVariable Long pieceId,
			@PathVariable Long ligneId) {
		return ResponseEntity.ok(pieceComptableService.getLigneById(pieceId, ligneId));
	}

	@PostMapping("/{pieceId}/lignes")
	public ResponseEntity<LignePieceComptable> createLigne(
			@PathVariable Long pieceId,
			@Valid @RequestBody LignePieceComptableRequest request) {
		LignePieceComptable created = pieceComptableService.createLigne(pieceId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{pieceId}/lignes/{ligneId}")
	public ResponseEntity<LignePieceComptable> updateLigne(
			@PathVariable Long pieceId,
			@PathVariable Long ligneId,
			@Valid @RequestBody LignePieceComptableRequest request) {
		return ResponseEntity.ok(pieceComptableService.updateLigne(pieceId, ligneId, request));
	}

	@DeleteMapping("/{pieceId}/lignes/{ligneId}")
	public ResponseEntity<Void> deleteLigne(
			@PathVariable Long pieceId,
			@PathVariable Long ligneId) {
		pieceComptableService.deleteLigne(pieceId, ligneId);
		return ResponseEntity.noContent().build();
	}
}
