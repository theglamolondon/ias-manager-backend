package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.services.CompteService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
public class CompteController {

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

    @GetMapping("/{id}")
    public ResponseEntity<Compte> getCompteById(@PathVariable Long id) {
        return ResponseEntity.ok(compteService.getCompteById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<Compte> getCompteByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(compteService.getCompteByNumero(numero));
    }

    @PostMapping
    public ResponseEntity<Compte> createCompte(@Valid @RequestBody CompteRequest request) {
        Compte created = compteService.createCompte(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compte> updateCompte(
            @PathVariable Long id,
            @Valid @RequestBody CompteRequest request) {
        return ResponseEntity.ok(compteService.updateCompte(id, request));
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

    @PostMapping("/{compteId}/lignes")
    public ResponseEntity<LigneCompte> createLigne(
            @PathVariable Long compteId,
            @Valid @RequestBody LigneCompteRequest request) {
        LigneCompte created = compteService.createLigne(compteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

}
