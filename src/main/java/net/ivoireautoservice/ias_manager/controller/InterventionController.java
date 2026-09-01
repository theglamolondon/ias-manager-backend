package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.services.InterventionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('INTERVENTION_READ')")
public class InterventionController {

    private final InterventionService interventionService;

    @GetMapping
    public ResponseEntity<PagedResponse<Intervention>> getAllInterventions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(interventionService.getAllInterventions(keyword, pageable));
    }

    @GetMapping("/vehicule/{vehiculeId}")
    public ResponseEntity<PagedResponse<Intervention>> getInterventionsByVehicule(
            @PathVariable Long vehiculeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "dhmsDebut") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(interventionService.getInterventionsByVehicule(vehiculeId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Intervention> getInterventionById(@PathVariable Long id) {
        return ResponseEntity.ok(interventionService.getInterventionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INTERVENTION_CREATE')")
    public ResponseEntity<Intervention> createIntervention(@Valid @RequestBody InterventionRequest request) {
        Intervention created = interventionService.createIntervention(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INTERVENTION_UPDATE')")
    public ResponseEntity<Intervention> updateIntervention(
            @PathVariable Long id,
            @Valid @RequestBody InterventionRequest request) {
        return ResponseEntity.ok(interventionService.updateIntervention(id, request));
    }

    @PatchMapping("/{id}/demarrer")
    @PreAuthorize("hasAuthority('INTERVENTION_UPDATE')")
    public ResponseEntity<Intervention> demarrerIntervention(@PathVariable Long id) {
        return ResponseEntity.ok(interventionService.demarrerIntervention(id));
    }

    @PatchMapping("/{id}/cloturer")
    @PreAuthorize("hasAuthority('INTERVENTION_UPDATE')")
    public ResponseEntity<Intervention> cloturerIntervention(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean vehiculeDisponible,
            @RequestParam(required = false) Long compteId) {
        return ResponseEntity.ok(interventionService.cloturerIntervention(id, vehiculeDisponible, compteId));
    }

    @PatchMapping("/{id}/payer")
    @PreAuthorize("hasAuthority('INTERVENTION_PAYER')")
    public ResponseEntity<Intervention> payerIntervention(
            @PathVariable Long id,
            @RequestParam Long compteId) {
        return ResponseEntity.ok(interventionService.payerIntervention(id, compteId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INTERVENTION_DELETE')")
    public ResponseEntity<Void> deleteIntervention(@PathVariable Long id) {
        interventionService.deleteIntervention(id);
        return ResponseEntity.noContent().build();
    }
}
