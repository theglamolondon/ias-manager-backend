package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.dto.request.UtilisateurRequest;
import net.ivoireautoservice.ias_manager.services.UtilisateurService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    public ResponseEntity<PagedResponse<UtilisateurDto>> getAllUtilisateurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(utilisateurService.getAllUtilisateurs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurDto> getUtilisateurById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getUtilisateurById(id));
    }

    @PostMapping
    public ResponseEntity<UtilisateurDto> createUtilisateur(@Valid @RequestBody UtilisateurRequest request) {
        UtilisateurDto created = utilisateurService.createUtilisateur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurDto> updateUtilisateur(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.updateUtilisateur(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.noContent().build();
    }
}