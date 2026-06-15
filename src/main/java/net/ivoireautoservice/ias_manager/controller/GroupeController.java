package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Groupe;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.GroupeRequest;
import net.ivoireautoservice.ias_manager.services.GroupeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groupes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GROUPE_MANAGE')")
public class GroupeController {

    private final GroupeService groupeService;

    @GetMapping
    public ResponseEntity<PagedResponse<Groupe>> getAllGroupes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(groupeService.getAllGroupes(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Groupe> getGroupeById(@PathVariable Long id) {
        return ResponseEntity.ok(groupeService.getGroupeById(id));
    }

    @PostMapping
    public ResponseEntity<Groupe> createGroupe(@Valid @RequestBody GroupeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupeService.createGroupe(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Groupe> updateGroupe(
            @PathVariable Long id,
            @Valid @RequestBody GroupeRequest request) {
        return ResponseEntity.ok(groupeService.updateGroupe(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupe(@PathVariable Long id) {
        groupeService.deleteGroupe(id);
        return ResponseEntity.noContent().build();
    }
}
