package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.DocumentVehicule;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Vehicule;
import net.ivoireautoservice.ias_manager.dto.core.VehiculeHistorique;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.services.VehiculeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/vehicules")
@RequiredArgsConstructor
public class VehiculeController {

    private final VehiculeService vehiculeService;

    @GetMapping
    public ResponseEntity<PagedResponse<Vehicule>> getAllVehicules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long assuranceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        VehiculeStatusEnum statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try { statutEnum = VehiculeStatusEnum.valueOf(statut); } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(vehiculeService.getAllVehicules(keyword, statutEnum, typeId, assuranceId, pageable));
    }

    @GetMapping("/{numChassis}")
    public ResponseEntity<Vehicule> getVehiculeByNumChassis(@PathVariable String numChassis) {
        return ResponseEntity.ok(vehiculeService.getVehiculeByNumChassis(numChassis));
    }

    @GetMapping("/immatriculation/{immatriculation}")
    public ResponseEntity<Vehicule> getVehiculeByImmatriculation(@PathVariable String immatriculation) {
        return ResponseEntity.ok(vehiculeService.getVehiculeByImmatriculation(immatriculation));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Vehicule>> getVehiculesByStatut(@PathVariable VehiculeStatusEnum statut) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByStatut(statut));
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<List<Vehicule>> getVehiculesByType(@PathVariable Long typeId) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByType(typeId));
    }

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<List<Vehicule>> getVehiculesByCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByCategorie(categorieId));
    }

    @PostMapping
    public ResponseEntity<Vehicule> createVehicule(@Valid @RequestBody VehiculeRequest request) {
        Vehicule created = vehiculeService.createVehicule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{vehiculeId}")
    public ResponseEntity<Vehicule> updateVehicule(
            @PathVariable Long vehiculeId,
            @Valid @RequestBody VehiculeRequest request) {
        return ResponseEntity.ok(vehiculeService.updateVehicule(vehiculeId, request));
    }

    @PatchMapping("/{vehiculeId}/statut")
    public ResponseEntity<Vehicule> updateStatut(
            @PathVariable Long vehiculeId,
            @RequestParam VehiculeStatusEnum statut) {
        return ResponseEntity.ok(vehiculeService.updateStatut(vehiculeId, statut));
    }

    // ==================== HISTORIQUE ====================

    @GetMapping("/historique/{numChassis}")
    public ResponseEntity<VehiculeHistorique> getHistorique(@PathVariable String numChassis) {
        return ResponseEntity.ok(vehiculeService.getHistorique(numChassis));
    }

    // ==================== INTERVENTIONS ====================

    @GetMapping("/{vehiculeId}/interventions")
    public ResponseEntity<PagedResponse<Intervention>> getInterventionsByVehicule(
            @PathVariable Long vehiculeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "dhmsDebut") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(vehiculeService.getInterventionsByVehicule(vehiculeId, pageable));
    }

    @PostMapping("/{vehiculeId}/interventions")
    public ResponseEntity<Intervention> createIntervention(
            @PathVariable Long vehiculeId,
            @Valid @RequestBody InterventionRequest request) {
        Intervention created = vehiculeService.createIntervention(vehiculeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== PHOTOS ====================

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Vehicule> updatePhotos(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile photoAvant,
            @RequestParam(required = false) MultipartFile photoArriere,
            @RequestParam(required = false) MultipartFile photoCoteDroit,
            @RequestParam(required = false) MultipartFile photoCoteGauche) {
        return ResponseEntity.ok(vehiculeService.updatePhotos(id, photoAvant, photoArriere, photoCoteDroit, photoCoteGauche));
    }

    // ==================== DOCUMENTS ====================

    @GetMapping("/{vehiculeId}/documents")
    public ResponseEntity<List<DocumentVehicule>> getDocuments(@PathVariable Long vehiculeId) {
        return ResponseEntity.ok(vehiculeService.getDocuments(vehiculeId));
    }

    @PostMapping(value = "/{vehiculeId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentVehicule> addDocument(
            @PathVariable Long vehiculeId,
            @RequestParam String label,
            @RequestParam MultipartFile file) {
        DocumentVehicule created = vehiculeService.addDocument(vehiculeId, label, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{vehiculeId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long vehiculeId,
            @PathVariable Long documentId) {
        vehiculeService.deleteDocument(vehiculeId, documentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{vehiculeId}")
    public ResponseEntity<Void> deleteVehicule(@PathVariable Long vehiculeId) {
        vehiculeService.deleteVehicule(vehiculeId);
        return ResponseEntity.noContent().build();
    }
}
