package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Partenaire;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.dto.request.PartenaireRequest;
import net.ivoireautoservice.ias_manager.services.PartenaireService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partenaires")
@RequiredArgsConstructor
public class PartenaireController {

    private final PartenaireService partenaireService;

    @GetMapping
    public ResponseEntity<PagedResponse<Partenaire>> getAllPartenaires(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(partenaireService.getAllPartenaires(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Partenaire> getPartenaireById(@PathVariable Long id) {
        return ResponseEntity.ok(partenaireService.getPartenaireById(id));
    }

    @GetMapping("/clients")
    public ResponseEntity<PagedResponse<Partenaire>> getClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(partenaireService.getClients(pageable));
    }

    @GetMapping("/fournisseurs")
    public ResponseEntity<PagedResponse<Partenaire>> getFournisseurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(partenaireService.getFournisseurs(pageable));
    }

    @PostMapping
    public ResponseEntity<Partenaire> createPartenaire(@Valid @RequestBody PartenaireRequest request) {
        Partenaire created = partenaireService.createPartenaire(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Partenaire> updatePartenaire(
            @PathVariable Long id,
            @Valid @RequestBody PartenaireRequest request) {
        return ResponseEntity.ok(partenaireService.updatePartenaire(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePartenaire(@PathVariable Long id) {
        partenaireService.deletePartenaire(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== BONS COMMANDE ====================

    @GetMapping("/{partenaireId}/bons-commande")
    public ResponseEntity<PagedResponse<BonCommande>> getBonsCommande(
            @PathVariable Long partenaireId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(partenaireService.getBonsCommandeByPartenaire(partenaireId, pageable));
    }

    @GetMapping("/{partenaireId}/bons-commande/{bonId}")
    public ResponseEntity<BonCommande> getBonCommandeById(
            @PathVariable Long partenaireId,
            @PathVariable Long bonId) {
        return ResponseEntity.ok(partenaireService.getBonCommandeById(partenaireId, bonId));
    }

    @PostMapping("/{partenaireId}/bons-commande")
    public ResponseEntity<BonCommande> createBonCommande(
            @PathVariable Long partenaireId,
            @Valid @RequestBody BonCommandeRequest request) {
        BonCommande created = partenaireService.createBonCommande(partenaireId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{partenaireId}/bons-commande/{bonId}")
    public ResponseEntity<Void> deleteBonCommande(
            @PathVariable Long partenaireId,
            @PathVariable Long bonId) {
        partenaireService.deleteBonCommande(partenaireId, bonId);
        return ResponseEntity.noContent().build();
    }
}
