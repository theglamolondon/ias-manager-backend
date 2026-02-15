package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.EntreeProduitRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.dto.request.SortieProduitRequest;
import net.ivoireautoservice.ias_manager.services.LivraisonService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/livraisons")
@RequiredArgsConstructor
public class LivraisonController {

    private final LivraisonService livraisonService;

    // ==================== LIVRAISONS CLIENT ====================

    @PostMapping("/clients/facture/{factureId}")
    public ResponseEntity<LivraisonClient> enregistrerLivraisonClient(@PathVariable Long factureId) {
        LivraisonClient created = livraisonService.enregistrerLivraisonClient(factureId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/clients")
    public ResponseEntity<PagedResponse<LivraisonClient>> getAllLivraisonsClient(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(livraisonService.getAllLivraisonsClient(pageable));
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<LivraisonClient> getLivraisonClientById(@PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraisonClientById(id));
    }

    @PostMapping("/clients")
    public ResponseEntity<LivraisonClient> createLivraisonClient(
            @Valid @RequestBody LivraisonClientRequest request) {
        LivraisonClient created = livraisonService.createLivraisonClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<LivraisonClient> updateLivraisonClient(
            @PathVariable Long id,
            @Valid @RequestBody LivraisonClientRequest request) {
        return ResponseEntity.ok(livraisonService.updateLivraisonClient(id, request));
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Void> deleteLivraisonClient(@PathVariable Long id) {
        livraisonService.deleteLivraisonClient(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== SORTIES PRODUIT ====================

    @GetMapping("/clients/{livraisonId}/sorties")
    public ResponseEntity<PagedResponse<SortieProduit>> getSortiesByLivraison(
            @PathVariable Long livraisonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(livraisonService.getSortiesByLivraison(livraisonId, pageable));
    }

    @PostMapping("/clients/{livraisonId}/sorties")
    public ResponseEntity<SortieProduit> createSortieProduit(
            @PathVariable Long livraisonId,
            @Valid @RequestBody SortieProduitRequest request) {
        SortieProduit created = livraisonService.createSortieProduit(livraisonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/clients/{livraisonId}/sorties/{sortieId}")
    public ResponseEntity<Void> deleteSortieProduit(
            @PathVariable Long livraisonId,
            @PathVariable Long sortieId) {
        livraisonService.deleteSortieProduit(livraisonId, sortieId);
        return ResponseEntity.noContent().build();
    }

    // ==================== LIVRAISONS FOURNISSEUR ====================

    @GetMapping("/fournisseurs")
    public ResponseEntity<PagedResponse<LivraisonFournisseur>> getAllLivraisonsFournisseur(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(livraisonService.getAllLivraisonsFournisseur(pageable));
    }

    @GetMapping("/fournisseurs/{id}")
    public ResponseEntity<LivraisonFournisseur> getLivraisonFournisseurById(@PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraisonFournisseurById(id));
    }

    @GetMapping("/fournisseurs/numero/{numero}")
    public ResponseEntity<LivraisonFournisseur> getLivraisonFournisseurByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(livraisonService.getLivraisonFournisseurByNumero(numero));
    }

    @PostMapping("/fournisseurs")
    public ResponseEntity<LivraisonFournisseur> createLivraisonFournisseur(
            @Valid @RequestBody LivraisonFournisseurRequest request) {
        LivraisonFournisseur created = livraisonService.createLivraisonFournisseur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/fournisseurs/{id}")
    public ResponseEntity<LivraisonFournisseur> updateLivraisonFournisseur(
            @PathVariable Long id,
            @Valid @RequestBody LivraisonFournisseurRequest request) {
        return ResponseEntity.ok(livraisonService.updateLivraisonFournisseur(id, request));
    }

    @DeleteMapping("/fournisseurs/{id}")
    public ResponseEntity<Void> deleteLivraisonFournisseur(@PathVariable Long id) {
        livraisonService.deleteLivraisonFournisseur(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ENTREES PRODUIT ====================

    @GetMapping("/fournisseurs/{livraisonId}/entrees")
    public ResponseEntity<PagedResponse<EntreeProduit>> getEntreesByLivraison(
            @PathVariable Long livraisonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(livraisonService.getEntreesByLivraison(livraisonId, pageable));
    }

    @PostMapping("/fournisseurs/{livraisonId}/entrees")
    public ResponseEntity<EntreeProduit> createEntreeProduit(
            @PathVariable Long livraisonId,
            @Valid @RequestBody EntreeProduitRequest request) {
        EntreeProduit created = livraisonService.createEntreeProduit(livraisonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/fournisseurs/{livraisonId}/entrees/{entreeId}")
    public ResponseEntity<Void> deleteEntreeProduit(
            @PathVariable Long livraisonId,
            @PathVariable Long entreeId) {
        livraisonService.deleteEntreeProduit(livraisonId, entreeId);
        return ResponseEntity.noContent().build();
    }
}
