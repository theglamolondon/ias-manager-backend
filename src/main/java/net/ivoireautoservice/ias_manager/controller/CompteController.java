package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.SyntheseCompte;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneImputationRequest;
import net.ivoireautoservice.ias_manager.services.CompteService;
import java.util.List;
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
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRESORERIE_READ')")
public class CompteController {

    /*
     * Lecture : TRESORERIE_READ suffit à entrer, mais le service restreint le
     * périmètre aux comptes rattachés à l'utilisateur — sauf pour un trésorier
     * en chef (TRESORERIE_ADMIN) qui voit l'intégralité des comptes.
     *
     * Administration des comptes (création, modification, affectation des
     * utilisateurs) : réservée à TRESORERIE_ADMIN.
     *
     * Mouvements (opération, solde) : TRESORERIE_CREATE / TRESORERIE_SOLDER,
     * plus une affectation explicite sur le compte concerné (vérifiée dans le
     * service) — un trésorier en chef non affecté ne peut que consulter.
     */

    private final CompteService compteService;


    @GetMapping
    public ResponseEntity<PagedResponse<Compte>> getAllComptes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(compteService.getAllComptes(keyword, pageable));
    }

    @GetMapping("/mes-comptes")
    public ResponseEntity<List<Compte>> getMesComptes(
            @RequestParam boolean factureClient) {
        return ResponseEntity.ok(compteService.getMesComptes(factureClient));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compte> getCompteById(@PathVariable Long id) {
        return ResponseEntity.ok(compteService.getCompteById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<Compte> getCompteByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(compteService.getCompteByNumero(numero));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TRESORERIE_ADMIN')")
    public ResponseEntity<Compte> createCompte(
            @RequestPart("request") @Valid CompteRequest request,
            @RequestPart(name = "logo", required = false) MultipartFile logo) {
        Compte created = compteService.createCompte(request, logo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TRESORERIE_ADMIN')")
    public ResponseEntity<Compte> updateCompte(
            @PathVariable Long id,
            @RequestPart("request") @Valid CompteRequest request,
            @RequestPart(name = "logo", required = false) MultipartFile logo) {
        return ResponseEntity.ok(compteService.updateCompte(id, request, logo));
    }

    @GetMapping("/{compteId}/lignes")
    public ResponseEntity<PagedResponse<LigneCompte>> getLignesByCompte(
            @PathVariable Long compteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "dhmsOperation") String tri,
            @RequestParam(defaultValue = "desc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(compteService.getLignesByCompte(compteId, pageable));
    }

    /**
     * Totaux du compte sur l'ensemble de ses opérations, indépendamment de la
     * pagination de {@link #getLignesByCompte}.
     */
    @GetMapping("/{compteId}/synthese")
    public ResponseEntity<SyntheseCompte> getSyntheseCompte(@PathVariable Long compteId) {
        return ResponseEntity.ok(compteService.getSyntheseCompte(compteId));
    }

    @PostMapping("/{compteId}/lignes")
    @PreAuthorize("hasAuthority('TRESORERIE_CREATE')")
    public ResponseEntity<LigneCompte> createLigne(
            @PathVariable Long compteId,
            @Valid @RequestBody LigneCompteRequest request) {
        LigneCompte created = compteService.createLigne(compteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Corrige l'imputation analytique d'un mouvement : sa nature et l'axe auquel il se
     * rattache. Le montant, le compte et le type sont hors d'atteinte — les modifier
     * fausserait la balance et les soldes intermédiaires de toutes les lignes suivantes.
     *
     * <p>Réservé au trésorier en chef ({@code TRESORERIE_ADMIN}) : réécrire une écriture
     * déjà passée reste un geste de contrôle, pas une opération courante de saisie.</p>
     */
    @PatchMapping("/{compteId}/lignes/{ligneId}/imputation")
    @PreAuthorize("hasAuthority('TRESORERIE_ADMIN')")
    public ResponseEntity<LigneCompte> updateImputation(
            @PathVariable Long compteId,
            @PathVariable Long ligneId,
            @Valid @RequestBody LigneImputationRequest request) {
        return ResponseEntity.ok(compteService.updateImputation(compteId, ligneId, request));
    }

    @PostMapping("/{compteId}/solder")
    @PreAuthorize("hasAuthority('TRESORERIE_SOLDER')")
    public ResponseEntity<LigneCompte> solderCompte(@PathVariable Long compteId) {
        LigneCompte created = compteService.solderCompte(compteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

}
