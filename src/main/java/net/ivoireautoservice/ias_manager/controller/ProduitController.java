package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.EntreeStock;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Produit;
import net.ivoireautoservice.ias_manager.dto.request.EntreeStockRequest;
import net.ivoireautoservice.ias_manager.dto.request.ProduitRequest;
import net.ivoireautoservice.ias_manager.services.ProduitService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
public class ProduitController {

    private final ProduitService produitService;

    @GetMapping
    public ResponseEntity<PagedResponse<Produit>> getAllProduits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(produitService.getAllProduits(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produit> getProduitById(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.getProduitById(id));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<Produit> getProduitByReference(@PathVariable String reference) {
        return ResponseEntity.ok(produitService.getProduitByReference(reference));
    }

    @GetMapping("/famille/{familleId}")
    public ResponseEntity<PagedResponse<Produit>> getProduitsByFamille(
            @PathVariable Long familleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(produitService.getProduitsByFamille(familleId, pageable));
    }

    @PostMapping
    public ResponseEntity<Produit> createProduit(@Valid @RequestBody ProduitRequest request) {
        Produit created = produitService.createProduit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produit> updateProduit(
            @PathVariable Long id,
            @Valid @RequestBody ProduitRequest request) {
        return ResponseEntity.ok(produitService.updateProduit(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entrees")
    public ResponseEntity<EntreeStock> enregistrerEntreeStock(@Valid @RequestBody EntreeStockRequest request) {
        EntreeStock created = produitService.enregistrerEntreeStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
