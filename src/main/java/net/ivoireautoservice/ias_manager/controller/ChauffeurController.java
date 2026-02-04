package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Chauffeur;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.ChauffeurRequest;
import net.ivoireautoservice.ias_manager.services.ChauffeurService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chauffeurs")
@RequiredArgsConstructor
public class ChauffeurController {

    private final ChauffeurService chauffeurService;

    @GetMapping
    public ResponseEntity<PagedResponse<Chauffeur>> getAllChauffeurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(chauffeurService.getAllChauffeurs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Chauffeur> getChauffeurById(@PathVariable Long id) {
        return ResponseEntity.ok(chauffeurService.getChauffeurById(id));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<Chauffeur> getChauffeurByEmployeId(@PathVariable Long employeId) {
        return ResponseEntity.ok(chauffeurService.getChauffeurByEmployeId(employeId));
    }

    @GetMapping("/permis/{numeroPermis}")
    public ResponseEntity<Chauffeur> getChauffeurByNumeroPermis(@PathVariable String numeroPermis) {
        return ResponseEntity.ok(chauffeurService.getChauffeurByNumeroPermis(numeroPermis));
    }

    @PostMapping
    public ResponseEntity<Chauffeur> createChauffeur(@Valid @RequestBody ChauffeurRequest request) {
        Chauffeur created = chauffeurService.createChauffeur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Chauffeur> updateChauffeur(
            @PathVariable Long id,
            @Valid @RequestBody ChauffeurRequest request) {
        return ResponseEntity.ok(chauffeurService.updateChauffeur(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChauffeur(@PathVariable Long id) {
        chauffeurService.deleteChauffeur(id);
        return ResponseEntity.noContent().build();
    }
}
