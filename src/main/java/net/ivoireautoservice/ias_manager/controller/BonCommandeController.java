package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.core.LigneBonCommande;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneBonCommandeRequest;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import net.ivoireautoservice.ias_manager.services.BonCommandeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bons-commande")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BON_COMMANDE_READ')")
public class BonCommandeController {

    private final BonCommandeService bonCommandeService;

    @GetMapping
    public ResponseEntity<PagedResponse<BonCommande>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long partenaireId,
            @RequestParam(required = false) BonCommandeStatusEnum statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "createdAt") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(bonCommandeService.getAll(keyword, partenaireId, statut, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonCommande> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bonCommandeService.getById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<BonCommande> getByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(bonCommandeService.getByNumero(numero));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BON_COMMANDE_CREATE')")
    public ResponseEntity<BonCommande> create(@Valid @RequestBody BonCommandeRequest request) {
        BonCommande created = bonCommandeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BON_COMMANDE_UPDATE')")
    public ResponseEntity<BonCommande> update(@PathVariable Long id,
                                              @Valid @RequestBody BonCommandeRequest request) {
        return ResponseEntity.ok(bonCommandeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BON_COMMANDE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bonCommandeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('BON_COMMANDE_UPDATE')")
    public ResponseEntity<BonCommande> valider(@PathVariable Long id) {
        return ResponseEntity.ok(bonCommandeService.valider(id));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('BON_COMMANDE_UPDATE')")
    public ResponseEntity<BonCommande> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(bonCommandeService.annuler(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        byte[] pdf = bonCommandeService.generatePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "bon-de-commande-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ==================== LIGNES BC ====================

    @GetMapping("/{id}/lignes")
    public ResponseEntity<List<LigneBonCommande>> getLignes(@PathVariable Long id) {
        return ResponseEntity.ok(bonCommandeService.getLignes(id));
    }

    @PostMapping("/{id}/lignes")
    @PreAuthorize("hasAuthority('BON_COMMANDE_UPDATE')")
    public ResponseEntity<LigneBonCommande> createLigne(@PathVariable Long id,
                                                       @Valid @RequestBody LigneBonCommandeRequest request) {
        LigneBonCommande created = bonCommandeService.createLigne(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('BON_COMMANDE_UPDATE')")
    public ResponseEntity<LigneBonCommande> updateLigne(@PathVariable Long id,
                                                        @PathVariable Long ligneId,
                                                        @Valid @RequestBody LigneBonCommandeRequest request) {
        return ResponseEntity.ok(bonCommandeService.updateLigne(id, ligneId, request));
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('BON_COMMANDE_UPDATE')")
    public ResponseEntity<Void> deleteLigne(@PathVariable Long id, @PathVariable Long ligneId) {
        bonCommandeService.deleteLigne(id, ligneId);
        return ResponseEntity.noContent().build();
    }
}
