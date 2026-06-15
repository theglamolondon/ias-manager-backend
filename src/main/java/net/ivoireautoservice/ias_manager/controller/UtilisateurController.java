package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.dto.request.AssignGroupesRequest;
import net.ivoireautoservice.ias_manager.dto.request.AssignRolesRequest;
import net.ivoireautoservice.ias_manager.dto.request.UtilisateurRequest;
import net.ivoireautoservice.ias_manager.services.UtilisateurService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('UTILISATEUR_READ')")
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
    @PreAuthorize("hasAuthority('UTILISATEUR_MANAGE')")
    public ResponseEntity<UtilisateurDto> createUtilisateur(@Valid @RequestBody UtilisateurRequest request) {
        UtilisateurDto created = utilisateurService.createUtilisateur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UTILISATEUR_MANAGE')")
    public ResponseEntity<UtilisateurDto> updateUtilisateur(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.updateUtilisateur(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('UTILISATEUR_MANAGE')")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    /** Remplace les rôles directs de l'utilisateur. */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('UTILISATEUR_MANAGE')")
    public ResponseEntity<UtilisateurDto> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(utilisateurService.assignRoles(id, request.getRoleIds()));
    }

    /** Remplace les groupes de l'utilisateur. */
    @PutMapping("/{id}/groupes")
    @PreAuthorize("hasAuthority('UTILISATEUR_MANAGE')")
    public ResponseEntity<UtilisateurDto> assignGroupes(
            @PathVariable Long id,
            @Valid @RequestBody AssignGroupesRequest request) {
        return ResponseEntity.ok(utilisateurService.assignGroupes(id, request.getGroupeIds()));
    }
}