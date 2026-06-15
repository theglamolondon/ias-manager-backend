package net.ivoireautoservice.ias_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Assurance;
import net.ivoireautoservice.ias_manager.dto.core.Categorie;
import net.ivoireautoservice.ias_manager.dto.core.FamilleProduit;
import net.ivoireautoservice.ias_manager.dto.core.Marque;
import net.ivoireautoservice.ias_manager.dto.core.Service;
import net.ivoireautoservice.ias_manager.dto.core.TypeAssurance;
import net.ivoireautoservice.ias_manager.dto.core.TypeCarburant;
import net.ivoireautoservice.ias_manager.dto.core.TypeDepense;
import net.ivoireautoservice.ias_manager.dto.core.TypeIntervention;
import net.ivoireautoservice.ias_manager.dto.core.TypeVehicule;
import net.ivoireautoservice.ias_manager.dto.request.CategorieRequest;
import net.ivoireautoservice.ias_manager.dto.request.FamilleProduitRequest;
import net.ivoireautoservice.ias_manager.dto.request.MarqueRequest;
import net.ivoireautoservice.ias_manager.dto.request.ServiceRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeAssuranceRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeCarburantRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeDepenseRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeInterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeVehiculeRequest;
import net.ivoireautoservice.ias_manager.services.SharedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Référentiels partagés (catégories, types, marques, assurances...).
 *
 * <p>Les lectures (GET) restent ouvertes à tout utilisateur authentifié car elles
 * alimentent les listes déroulantes des formulaires de tous les modules. Seules les
 * écritures sont réservées à la gestion des paramètres ({@code PARAMETRE_MANAGE}).</p>
 */
@RestController
@RequestMapping("/api/commons")
@RequiredArgsConstructor
public class SharedController {

    private final SharedService sharedService;

    // ==================== CATEGORIES ====================

    @GetMapping("/categories")
    public ResponseEntity<List<Categorie>> getAllCategories() {
        return ResponseEntity.ok(sharedService.getAllCategories());
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<Categorie> getCategorieById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getCategorieById(id));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Categorie> createCategorie(@Valid @RequestBody CategorieRequest request) {
        Categorie created = sharedService.createCategorie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Categorie> updateCategorie(
            @PathVariable Long id,
            @Valid @RequestBody CategorieRequest request) {
        return ResponseEntity.ok(sharedService.updateCategorie(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteCategorie(@PathVariable Long id) {
        sharedService.deleteCategorie(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPES VEHICULE ====================

    @GetMapping("/types-vehicule")
    public ResponseEntity<List<TypeVehicule>> getAllTypesVehicule() {
        return ResponseEntity.ok(sharedService.getAllTypesVehicule());
    }

    @GetMapping("/types-vehicule/{id}")
    public ResponseEntity<TypeVehicule> getTypeVehiculeById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getTypeVehiculeById(id));
    }

    @PostMapping("/types-vehicule")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeVehicule> createTypeVehicule(@Valid @RequestBody TypeVehiculeRequest request) {
        TypeVehicule created = sharedService.createTypeVehicule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/types-vehicule/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeVehicule> updateTypeVehicule(
            @PathVariable Long id,
            @Valid @RequestBody TypeVehiculeRequest request) {
        return ResponseEntity.ok(sharedService.updateTypeVehicule(id, request));
    }

    @DeleteMapping("/types-vehicule/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteTypeVehicule(@PathVariable Long id) {
        sharedService.deleteTypeVehicule(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== SERVICES ====================

    @GetMapping("/services")
    public ResponseEntity<List<Service>> getAllServices() {
        return ResponseEntity.ok(sharedService.getAllServices());
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<Service> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getServiceById(id));
    }

    @PostMapping("/services")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Service> createService(@Valid @RequestBody ServiceRequest request) {
        Service created = sharedService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Service> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(sharedService.updateService(id, request));
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        sharedService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPES INTERVENTION ====================

    @GetMapping("/types-intervention")
    public ResponseEntity<List<TypeIntervention>> getAllTypesIntervention() {
        return ResponseEntity.ok(sharedService.getAllTypesIntervention());
    }

    @GetMapping("/types-intervention/{id}")
    public ResponseEntity<TypeIntervention> getTypeInterventionById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getTypeInterventionById(id));
    }

    @PostMapping("/types-intervention")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeIntervention> createTypeIntervention(@Valid @RequestBody TypeInterventionRequest request) {
        TypeIntervention created = sharedService.createTypeIntervention(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/types-intervention/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeIntervention> updateTypeIntervention(
            @PathVariable Long id,
            @Valid @RequestBody TypeInterventionRequest request) {
        return ResponseEntity.ok(sharedService.updateTypeIntervention(id, request));
    }

    @DeleteMapping("/types-intervention/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteTypeIntervention(@PathVariable Long id) {
        sharedService.deleteTypeIntervention(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FAMILLES PRODUIT ====================

    @GetMapping("/familles-produit")
    public ResponseEntity<List<FamilleProduit>> getAllFamillesProduit() {
        return ResponseEntity.ok(sharedService.getAllFamillesProduit());
    }

    @GetMapping("/familles-produit/{id}")
    public ResponseEntity<FamilleProduit> getFamilleProduitById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getFamilleProduitById(id));
    }

    @PostMapping("/familles-produit")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<FamilleProduit> createFamilleProduit(@Valid @RequestBody FamilleProduitRequest request) {
        FamilleProduit created = sharedService.createFamilleProduit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/familles-produit/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<FamilleProduit> updateFamilleProduit(
            @PathVariable Long id,
            @Valid @RequestBody FamilleProduitRequest request) {
        return ResponseEntity.ok(sharedService.updateFamilleProduit(id, request));
    }

    @DeleteMapping("/familles-produit/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteFamilleProduit(@PathVariable Long id) {
        sharedService.deleteFamilleProduit(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPES DEPENSE ====================

    @GetMapping("/types-depense")
    public ResponseEntity<List<TypeDepense>> getAllTypesDepense() {
        return ResponseEntity.ok(sharedService.getAllTypesDepense());
    }

    @GetMapping("/types-depense/{id}")
    public ResponseEntity<TypeDepense> getTypeDepenseById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getTypeDepenseById(id));
    }

    @PostMapping("/types-depense")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeDepense> createTypeDepense(@Valid @RequestBody TypeDepenseRequest request) {
        TypeDepense created = sharedService.createTypeDepense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/types-depense/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeDepense> updateTypeDepense(
            @PathVariable Long id,
            @Valid @RequestBody TypeDepenseRequest request) {
        return ResponseEntity.ok(sharedService.updateTypeDepense(id, request));
    }

    @DeleteMapping("/types-depense/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteTypeDepense(@PathVariable Long id) {
        sharedService.deleteTypeDepense(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== TYPES CARBURANT ====================

    @GetMapping("/types-carburant")
    public ResponseEntity<List<TypeCarburant>> getAllTypesCarburant() {
        return ResponseEntity.ok(sharedService.getAllTypesCarburant());
    }

    @PostMapping("/types-carburant")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeCarburant> createTypeCarburant(@Valid @RequestBody TypeCarburantRequest request) {
        TypeCarburant created = sharedService.createTypeCarburant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== TYPES ASSURANCE ====================

    @GetMapping("/types-assurance")
    public ResponseEntity<List<TypeAssurance>> getAllTypesAssurance() {
        return ResponseEntity.ok(sharedService.getAllTypesAssurance());
    }

    @GetMapping("/types-assurance/{id}")
    public ResponseEntity<TypeAssurance> getTypeAssuranceById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getTypeAssuranceById(id));
    }

    @PostMapping("/types-assurance")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeAssurance> createTypeAssurance(@Valid @RequestBody TypeAssuranceRequest request) {
        TypeAssurance created = sharedService.createTypeAssurance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/types-assurance/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<TypeAssurance> updateTypeAssurance(
            @PathVariable Long id,
            @Valid @RequestBody TypeAssuranceRequest request) {
        return ResponseEntity.ok(sharedService.updateTypeAssurance(id, request));
    }

    // ==================== MARQUES ====================

    @GetMapping("/marques")
    public ResponseEntity<List<Marque>> getAllMarques() {
        return ResponseEntity.ok(sharedService.getAllMarques());
    }

    @PostMapping("/marques")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Marque> createMarque(@Valid @RequestBody MarqueRequest request) {
        Marque created = sharedService.createMarque(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== ASSURANCES ====================

    @GetMapping("/assurances")
    public ResponseEntity<List<Assurance>> getAllAssurances() {
        return ResponseEntity.ok(sharedService.getAllAssurances());
    }

    @GetMapping("/assurances/{id}")
    public ResponseEntity<Assurance> getAssuranceById(@PathVariable Long id) {
        return ResponseEntity.ok(sharedService.getAssuranceById(id));
    }

    @PostMapping(value = "/assurances", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Assurance> createAssurance(
            @RequestParam String libelle,
            @RequestParam(required = false) MultipartFile logo) {
        Assurance created = sharedService.createAssurance(libelle, logo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/assurances/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Assurance> updateAssurance(
            @PathVariable Long id,
            @RequestParam String libelle,
            @RequestParam(required = false) MultipartFile logo) {
        return ResponseEntity.ok(sharedService.updateAssurance(id, libelle, logo));
    }

    @DeleteMapping("/assurances/{id}")
    @PreAuthorize("hasAuthority('PARAMETRE_MANAGE')")
    public ResponseEntity<Void> deleteAssurance(@PathVariable Long id) {
        sharedService.deleteAssurance(id);
        return ResponseEntity.noContent().build();
    }
}
