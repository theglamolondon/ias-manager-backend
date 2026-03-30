package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClientSummary;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseurSummary;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.services.LivraisonService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public ResponseEntity<PagedResponse<LivraisonClientSummary>> getAllLivraisonsClient(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "createdAt") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(livraisonService.getAllLivraisonsClient(keyword, pageable));
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<LivraisonClient> getLivraisonClientById(@PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraisonClientById(id));
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Void> deleteLivraisonClient(@PathVariable Long id) {
        livraisonService.deleteLivraisonClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients/{id}/pdf")
    public ResponseEntity<byte[]> generateBonLivraisonClientPdf(@PathVariable Long id) {
        byte[] pdf = livraisonService.generateBonLivraisonClientPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "bon-livraison-client-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ==================== LIVRAISONS FOURNISSEUR ====================

    @GetMapping("/fournisseurs")
    public ResponseEntity<PagedResponse<LivraisonFournisseurSummary>> getAllLivraisonsFournisseur(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "createdAt") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(livraisonService.getAllLivraisonsFournisseur(keyword, pageable));
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


    @GetMapping("/fournisseurs/{id}/pdf")
    public ResponseEntity<byte[]> generateBonLivraisonFournisseurPdf(@PathVariable Long id) {
        byte[] pdf = livraisonService.generateBonLivraisonFournisseurPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "bon-livraison-fournisseur-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
