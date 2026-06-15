package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClientSummary;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseurSummary;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.AnnulationLivraisonRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.services.LivraisonService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/livraisons")
@RequiredArgsConstructor
public class LivraisonController {

    private final LivraisonService livraisonService;

    // ==================== LIVRAISONS CLIENT ====================

    @PostMapping("/clients/facture/{factureId}")
    @PreAuthorize("hasAuthority('LIVRAISON_CLIENT_CREATE')")
    public ResponseEntity<LivraisonClient> enregistrerLivraisonClient(
            @PathVariable Long factureId,
            @RequestBody(required = false) LivraisonClientRequest request) {
        LivraisonClient created = livraisonService.enregistrerLivraisonClient(factureId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('LIVRAISON_CLIENT_READ')")
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
    @PreAuthorize("hasAuthority('LIVRAISON_CLIENT_READ')")
    public ResponseEntity<LivraisonClient> getLivraisonClientById(@PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraisonClientById(id));
    }

    @DeleteMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('LIVRAISON_CLIENT_DELETE')")
    public ResponseEntity<Void> deleteLivraisonClient(@PathVariable Long id) {
        livraisonService.deleteLivraisonClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients/{id}/pdf")
    @PreAuthorize("hasAuthority('LIVRAISON_CLIENT_READ')")
    public ResponseEntity<byte[]> generateBonLivraisonClientPdf(@PathVariable Long id) {
        byte[] pdf = livraisonService.generateBonLivraisonClientPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "bon-livraison-client-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ==================== LIVRAISONS FOURNISSEUR ====================

    @GetMapping("/fournisseurs")
    @PreAuthorize("hasAuthority('APPRO_READ')")
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
    @PreAuthorize("hasAuthority('APPRO_READ')")
    public ResponseEntity<LivraisonFournisseur> getLivraisonFournisseurById(@PathVariable Long id) {
        return ResponseEntity.ok(livraisonService.getLivraisonFournisseurById(id));
    }

    @GetMapping("/fournisseurs/facturables")
    @PreAuthorize("hasAuthority('APPRO_READ')")
    public ResponseEntity<java.util.List<LivraisonFournisseurSummary>> getLivraisonsFournisseurFacturables(
            @RequestParam(required = false) Long partenaireId) {
        return ResponseEntity.ok(livraisonService.getLivraisonsFournisseurFacturables(partenaireId));
    }

    @GetMapping("/fournisseurs/numero/{numero}")
    @PreAuthorize("hasAuthority('APPRO_READ')")
    public ResponseEntity<LivraisonFournisseur> getLivraisonFournisseurByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(livraisonService.getLivraisonFournisseurByNumero(numero));
    }

    @PostMapping("/fournisseurs")
    @PreAuthorize("hasAuthority('APPRO_CREATE')")
    public ResponseEntity<LivraisonFournisseur> createLivraisonFournisseur(
            @Valid @RequestBody LivraisonFournisseurRequest request) {
        LivraisonFournisseur created = livraisonService.createLivraisonFournisseur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/fournisseurs/facture/{factureId}")
    @PreAuthorize("hasAuthority('APPRO_CREATE')")
    public ResponseEntity<LivraisonFournisseur> enregistrerLivraisonFournisseurFromFacture(
            @PathVariable Long factureId) {
        LivraisonFournisseur created = livraisonService.enregistrerLivraisonFournisseurFromFacture(factureId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/fournisseurs/{id}/valider")
    @PreAuthorize("hasAuthority('APPRO_UPDATE')")
    public ResponseEntity<LivraisonFournisseur> validerLivraisonFournisseur(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean facturerMaintenant) {
        return ResponseEntity.ok(livraisonService.validerLivraisonFournisseur(id, facturerMaintenant));
    }

    @PatchMapping("/fournisseurs/{id}/annuler")
    @PreAuthorize("hasAuthority('APPRO_UPDATE')")
    public ResponseEntity<LivraisonFournisseur> annulerLivraisonFournisseur(
            @PathVariable Long id,
            @Valid @RequestBody AnnulationLivraisonRequest request) {
        return ResponseEntity.ok(livraisonService.annulerLivraisonFournisseur(id, request.getStatutBcCible()));
    }

    @GetMapping("/fournisseurs/{id}/pdf")
    @PreAuthorize("hasAuthority('APPRO_READ')")
    public ResponseEntity<byte[]> generateBonLivraisonFournisseurPdf(@PathVariable Long id) {
        byte[] pdf = livraisonService.generateBonLivraisonFournisseurPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "bon-livraison-fournisseur-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
