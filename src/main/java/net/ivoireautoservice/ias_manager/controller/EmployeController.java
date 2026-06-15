package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Employe;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.EmployeRequest;
import net.ivoireautoservice.ias_manager.services.EmployeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('EMPLOYE_READ')")
public class EmployeController {

    private final EmployeService employeService;

    @GetMapping
    public ResponseEntity<PagedResponse<Employe>> getAllEmployes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille,
            @RequestParam(defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String ordre) {
        Sort sort = ordre.equalsIgnoreCase("desc") ? Sort.by(tri).descending() : Sort.by(tri).ascending();
        Pageable pageable = PageRequest.of(page, taille, sort);
        return ResponseEntity.ok(employeService.getAllEmployes(keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employe> getEmployeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeService.getEmployeById(id));
    }

    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<Employe> getEmployeByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(employeService.getEmployeByMatricule(matricule));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYE_CREATE')")
    public ResponseEntity<Employe> createEmploye(@Valid @RequestBody EmployeRequest request) {
        Employe created = employeService.createEmploye(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYE_UPDATE')")
    public ResponseEntity<Employe> updateEmploye(
            @PathVariable Long id,
            @Valid @RequestBody EmployeRequest request) {
        return ResponseEntity.ok(employeService.updateEmploye(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYE_DELETE')")
    public ResponseEntity<Void> deleteEmploye(@PathVariable Long id) {
        employeService.deleteEmploye(id);
        return ResponseEntity.noContent().build();
    }
}
