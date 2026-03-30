package net.ivoireautoservice.ias_manager.controller;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.services.StatistiqueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
public class StatistiqueController {

	private final StatistiqueService statistiqueService;

	@GetMapping("/dashboard")
	public ResponseEntity<StatistiqueDashboard> getDashboard(
			@RequestParam(required = false) Integer annee) {
		if (annee == null) {
			annee = LocalDate.now().getYear();
		}
		return ResponseEntity.ok(statistiqueService.getDashboard(annee));
	}


	@GetMapping("/vehicules")
	public ResponseEntity<VehiculeStats> getVehiculeStats() {
		return ResponseEntity.ok(statistiqueService.getVehiculeStats());
	}

	@GetMapping("/missions")
	public ResponseEntity<MissionStats> getMissionStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		if (dateDebut == null) dateDebut = LocalDate.of(LocalDate.now().getYear(), 1, 1);
		if (dateFin == null) dateFin = LocalDate.of(LocalDate.now().getYear(), 12, 31);
		return ResponseEntity.ok(statistiqueService.getMissionStats(dateDebut, dateFin));
	}

	@GetMapping("/interventions")
	public ResponseEntity<InterventionStats> getInterventionStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		if (dateDebut == null) dateDebut = LocalDate.of(LocalDate.now().getYear(), 1, 1);
		if (dateFin == null) dateFin = LocalDate.of(LocalDate.now().getYear(), 12, 31);
		return ResponseEntity.ok(statistiqueService.getInterventionStats(dateDebut, dateFin));
	}

	@GetMapping("/chauffeurs")
	public ResponseEntity<ChauffeurStats> getChauffeurStats() {
		return ResponseEntity.ok(statistiqueService.getChauffeurStats());
	}

	@GetMapping("/produits")
	public ResponseEntity<ProduitStats> getProduitStats() {
		return ResponseEntity.ok(statistiqueService.getProduitStats());
	}

	@GetMapping("/livraisons/fournisseurs")
	public ResponseEntity<LivraisonFournisseurStats> getLivraisonFournisseurStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		if (dateDebut == null) dateDebut = LocalDate.of(LocalDate.now().getYear(), 1, 1);
		if (dateFin == null) dateFin = LocalDate.of(LocalDate.now().getYear(), 12, 31);
		return ResponseEntity.ok(statistiqueService.getLivraisonFournisseurStats(dateDebut, dateFin));
	}

	@GetMapping("/livraisons/clients")
	public ResponseEntity<LivraisonClientStats> getLivraisonClientStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		if (dateDebut == null) dateDebut = LocalDate.of(LocalDate.now().getYear(), 1, 1);
		if (dateFin == null) dateFin = LocalDate.of(LocalDate.now().getYear(), 12, 31);
		return ResponseEntity.ok(statistiqueService.getLivraisonClientStats(dateDebut, dateFin));
	}

	@GetMapping("/comptes")
	public ResponseEntity<CompteStats> getCompteStats() {
		return ResponseEntity.ok(statistiqueService.getCompteStats());
	}

	@GetMapping("/rapport-financier")
	public ResponseEntity<RapportFinancier> getRapportFinancier(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		java.time.LocalDateTime debut = dateDebut != null ? dateDebut.atStartOfDay() : null;
		java.time.LocalDateTime fin = dateFin != null ? dateFin.plusDays(1).atStartOfDay() : null;
		return ResponseEntity.ok(statistiqueService.getRapportFinancier(debut, fin));
	}

	@GetMapping("/factures/fournisseurs")
	public ResponseEntity<FactureStats> getFactureFournisseurStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		if (dateDebut == null) dateDebut = LocalDate.of(LocalDate.now().getYear(), 1, 1);
		if (dateFin == null) dateFin = LocalDate.of(LocalDate.now().getYear(), 12, 31);
		return ResponseEntity.ok(statistiqueService.getFactureStats(false, dateDebut, dateFin));
	}

	@GetMapping("/factures/clients")
	public ResponseEntity<FactureStats> getFactureClientStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
		if (dateDebut == null) dateDebut = LocalDate.of(LocalDate.now().getYear(), 1, 1);
		if (dateFin == null) dateFin = LocalDate.of(LocalDate.now().getYear(), 12, 31);
		return ResponseEntity.ok(statistiqueService.getFactureStats(true, dateDebut, dateFin));
	}

	@GetMapping("/recap-mensuel")
	public ResponseEntity<RecapitulatifMensuel> getRecapitulatifMensuel(
			@RequestParam(required = false) Integer annee) {
		if (annee == null) {
			annee = LocalDate.now().getYear();
		}
		return ResponseEntity.ok(statistiqueService.getRecapitulatifMensuel(annee));
	}
}
