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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PRODUIT_READ')")
public class ProduitController {

    private final ProduitService produitService;

    @GetMapping
    public ResponseEntity<PagedResponse<Produit>> getAllProduits(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "designation") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(produitService.getAllProduits(keyword, pageable));
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUIT_CREATE')")
    public ResponseEntity<Produit> createProduit(
            @Valid @ModelAttribute ProduitRequest request,
            @RequestParam(required = false) MultipartFile image) {
        Produit created = produitService.createProduit(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUIT_UPDATE')")
    public ResponseEntity<Produit> updateProduit(
            @PathVariable Long id,
            @Valid @ModelAttribute ProduitRequest request,
            @RequestParam(required = false) MultipartFile image) {
        return ResponseEntity.ok(produitService.updateProduit(id, request, image));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUIT_DELETE')")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entrees")
    @PreAuthorize("hasAuthority('PRODUIT_UPDATE')")
    public ResponseEntity<EntreeStock> enregistrerEntreeStock(@Valid @RequestBody EntreeStockRequest request) {
        EntreeStock created = produitService.enregistrerEntreeStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
