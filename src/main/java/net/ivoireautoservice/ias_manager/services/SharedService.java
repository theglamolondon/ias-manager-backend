package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Categorie;
import net.ivoireautoservice.ias_manager.dto.core.FamilleProduit;
import net.ivoireautoservice.ias_manager.dto.core.Service;
import net.ivoireautoservice.ias_manager.dto.core.TypeDepense;
import net.ivoireautoservice.ias_manager.dto.core.TypeIntervention;
import net.ivoireautoservice.ias_manager.dto.core.TypeStatutPieceComptable;
import net.ivoireautoservice.ias_manager.dto.core.TypeVehicule;
import net.ivoireautoservice.ias_manager.dto.request.CategorieRequest;
import net.ivoireautoservice.ias_manager.dto.request.FamilleProduitRequest;
import net.ivoireautoservice.ias_manager.dto.request.ServiceRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeDepenseRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeInterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeStatutPieceComptableRequest;
import net.ivoireautoservice.ias_manager.dto.request.TypeVehiculeRequest;
import net.ivoireautoservice.ias_manager.entity.CategorieEntity;
import net.ivoireautoservice.ias_manager.entity.FamilleProduitEntity;
import net.ivoireautoservice.ias_manager.entity.ServiceEntity;
import net.ivoireautoservice.ias_manager.entity.TypeDepenseEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.TypeStatutPieceComptableEntity;
import net.ivoireautoservice.ias_manager.entity.TypeVehiculeEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.CategorieMapper;
import net.ivoireautoservice.ias_manager.mapper.FamilleProduitMapper;
import net.ivoireautoservice.ias_manager.mapper.ServiceMapper;
import net.ivoireautoservice.ias_manager.mapper.TypeDepenseMapper;
import net.ivoireautoservice.ias_manager.mapper.TypeInterventionMapper;
import net.ivoireautoservice.ias_manager.mapper.TypeStatutPieceComptableMapper;
import net.ivoireautoservice.ias_manager.mapper.TypeVehiculeMapper;
import net.ivoireautoservice.ias_manager.repository.CategoryRepository;
import net.ivoireautoservice.ias_manager.repository.FamilleProduitRepository;
import net.ivoireautoservice.ias_manager.repository.ServiceRepository;
import net.ivoireautoservice.ias_manager.repository.TypeDepenseRepository;
import net.ivoireautoservice.ias_manager.repository.TypeInterventionRepository;
import net.ivoireautoservice.ias_manager.repository.TypeStatutPieceComptableRepository;
import net.ivoireautoservice.ias_manager.repository.TypeVehiculeRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SharedService {

    private final CategoryRepository categoryRepository;
    private final TypeVehiculeRepository typeVehiculeRepository;
    private final ServiceRepository serviceRepository;
    private final TypeInterventionRepository typeInterventionRepository;
    private final FamilleProduitRepository familleProduitRepository;
    private final TypeStatutPieceComptableRepository typeStatutPieceComptableRepository;
    private final TypeDepenseRepository typeDepenseRepository;
    private final CategorieMapper categorieMapper;
    private final TypeVehiculeMapper typeVehiculeMapper;
    private final ServiceMapper serviceMapper;
    private final TypeInterventionMapper typeInterventionMapper;
    private final FamilleProduitMapper familleProduitMapper;
    private final TypeStatutPieceComptableMapper typeStatutPieceComptableMapper;
    private final TypeDepenseMapper typeDepenseMapper;

    // ==================== CATEGORIES ====================

    @Transactional(readOnly = true)
    public List<Categorie> getAllCategories() {
        return categorieMapper.toDtoList(categoryRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Categorie getCategorieById(Long id) {
        CategorieEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
        return categorieMapper.toDto(entity);
    }

    @Transactional
    public Categorie createCategorie(CategorieRequest request) {
        CategorieEntity entity = categorieMapper.toEntity(request);
        CategorieEntity saved = categoryRepository.save(entity);
        return categorieMapper.toDto(saved);
    }

    @Transactional
    public Categorie updateCategorie(Long id, CategorieRequest request) {
        CategorieEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
        categorieMapper.updateEntity(request, entity);
        CategorieEntity saved = categoryRepository.save(entity);
        return categorieMapper.toDto(saved);
    }

    @Transactional
    public void deleteCategorie(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie", id);
        }
        categoryRepository.deleteById(id);
    }

    // ==================== TYPES VEHICULE ====================

    @Transactional(readOnly = true)
    public List<TypeVehicule> getAllTypesVehicule() {
        return typeVehiculeMapper.toDtoList(typeVehiculeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TypeVehicule getTypeVehiculeById(Long id) {
        TypeVehiculeEntity entity = typeVehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de véhicule", id));
        return typeVehiculeMapper.toDto(entity);
    }

    @Transactional
    public TypeVehicule createTypeVehicule(TypeVehiculeRequest request) {
        CategorieEntity categorie = categoryRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", request.getCategorieId()));

        TypeVehiculeEntity entity = typeVehiculeMapper.toEntity(request);
        entity.setCategorie(categorie);

        TypeVehiculeEntity saved = typeVehiculeRepository.save(entity);
        return typeVehiculeMapper.toDto(saved);
    }

    @Transactional
    public TypeVehicule updateTypeVehicule(Long id, TypeVehiculeRequest request) {
        TypeVehiculeEntity entity = typeVehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de véhicule", id));

        CategorieEntity categorie = categoryRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", request.getCategorieId()));

        typeVehiculeMapper.updateEntity(request, entity);
        entity.setCategorie(categorie);

        TypeVehiculeEntity saved = typeVehiculeRepository.save(entity);
        return typeVehiculeMapper.toDto(saved);
    }

    @Transactional
    public void deleteTypeVehicule(Long id) {
        if (!typeVehiculeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Type de véhicule", id);
        }
        typeVehiculeRepository.deleteById(id);
    }

    // ==================== SERVICES ====================

    @Transactional(readOnly = true)
    public List<Service> getAllServices() {
        return serviceMapper.toDtoList(serviceRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Service getServiceById(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", id));
        return serviceMapper.toDto(entity);
    }

    @Transactional
    public Service createService(ServiceRequest request) {
        ServiceEntity entity = serviceMapper.toEntity(request);
        ServiceEntity saved = serviceRepository.save(entity);
        return serviceMapper.toDto(saved);
    }

    @Transactional
    public Service updateService(Long id, ServiceRequest request) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", id));
        serviceMapper.updateEntity(request, entity);
        ServiceEntity saved = serviceRepository.save(entity);
        return serviceMapper.toDto(saved);
    }

    @Transactional
    public void deleteService(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service", id);
        }
        serviceRepository.deleteById(id);
    }

    // ==================== TYPES INTERVENTION ====================

    @Transactional(readOnly = true)
    public List<TypeIntervention> getAllTypesIntervention() {
        return typeInterventionMapper.toDtoList(typeInterventionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TypeIntervention getTypeInterventionById(Long id) {
        TypeInterventionEntity entity = typeInterventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'intervention", id));
        return typeInterventionMapper.toDto(entity);
    }

    @Transactional
    public TypeIntervention createTypeIntervention(TypeInterventionRequest request) {
        TypeInterventionEntity entity = typeInterventionMapper.toEntity(request);
        TypeInterventionEntity saved = typeInterventionRepository.save(entity);
        return typeInterventionMapper.toDto(saved);
    }

    @Transactional
    public TypeIntervention updateTypeIntervention(Long id, TypeInterventionRequest request) {
        TypeInterventionEntity entity = typeInterventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'intervention", id));
        typeInterventionMapper.updateEntity(request, entity);
        TypeInterventionEntity saved = typeInterventionRepository.save(entity);
        return typeInterventionMapper.toDto(saved);
    }

    @Transactional
    public void deleteTypeIntervention(Long id) {
        if (!typeInterventionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Type d'intervention", id);
        }
        typeInterventionRepository.deleteById(id);
    }

    // ==================== FAMILLES PRODUIT ====================

    @Transactional(readOnly = true)
    public List<FamilleProduit> getAllFamillesProduit() {
        return familleProduitMapper.toDtoList(familleProduitRepository.findAll());
    }

    @Transactional(readOnly = true)
    public FamilleProduit getFamilleProduitById(Long id) {
        FamilleProduitEntity entity = familleProduitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Famille de produit", id));
        return familleProduitMapper.toDto(entity);
    }

    @Transactional
    public FamilleProduit createFamilleProduit(FamilleProduitRequest request) {
        FamilleProduitEntity entity = familleProduitMapper.toEntity(request);
        FamilleProduitEntity saved = familleProduitRepository.save(entity);
        return familleProduitMapper.toDto(saved);
    }

    @Transactional
    public FamilleProduit updateFamilleProduit(Long id, FamilleProduitRequest request) {
        FamilleProduitEntity entity = familleProduitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Famille de produit", id));
        familleProduitMapper.updateEntity(request, entity);
        FamilleProduitEntity saved = familleProduitRepository.save(entity);
        return familleProduitMapper.toDto(saved);
    }

    @Transactional
    public void deleteFamilleProduit(Long id) {
        if (!familleProduitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Famille de produit", id);
        }
        familleProduitRepository.deleteById(id);
    }

    // ==================== TYPES STATUT PIECE COMPTABLE ====================

    @Transactional(readOnly = true)
    public List<TypeStatutPieceComptable> getAllTypesStatutPieceComptable() {
        return typeStatutPieceComptableMapper.toDtoList(typeStatutPieceComptableRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TypeStatutPieceComptable getTypeStatutPieceComptableById(Long id) {
        TypeStatutPieceComptableEntity entity = typeStatutPieceComptableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de statut pièce comptable", id));
        return typeStatutPieceComptableMapper.toDto(entity);
    }

    @Transactional
    public TypeStatutPieceComptable createTypeStatutPieceComptable(TypeStatutPieceComptableRequest request) {
        TypeStatutPieceComptableEntity entity = typeStatutPieceComptableMapper.toEntity(request);
        TypeStatutPieceComptableEntity saved = typeStatutPieceComptableRepository.save(entity);
        return typeStatutPieceComptableMapper.toDto(saved);
    }

    @Transactional
    public TypeStatutPieceComptable updateTypeStatutPieceComptable(Long id, TypeStatutPieceComptableRequest request) {
        TypeStatutPieceComptableEntity entity = typeStatutPieceComptableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de statut pièce comptable", id));
        typeStatutPieceComptableMapper.updateEntity(request, entity);
        TypeStatutPieceComptableEntity saved = typeStatutPieceComptableRepository.save(entity);
        return typeStatutPieceComptableMapper.toDto(saved);
    }

    @Transactional
    public void deleteTypeStatutPieceComptable(Long id) {
        if (!typeStatutPieceComptableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Type de statut pièce comptable", id);
        }
        typeStatutPieceComptableRepository.deleteById(id);
    }

    // ==================== TYPES DEPENSE ====================

    @Transactional(readOnly = true)
    public List<TypeDepense> getAllTypesDepense() {
        return typeDepenseMapper.toDtoList(typeDepenseRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TypeDepense getTypeDepenseById(Long id) {
        TypeDepenseEntity entity = typeDepenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de dépense", id));
        return typeDepenseMapper.toDto(entity);
    }

    @Transactional
    public TypeDepense createTypeDepense(TypeDepenseRequest request) {
        TypeDepenseEntity entity = typeDepenseMapper.toEntity(request);
        TypeDepenseEntity saved = typeDepenseRepository.save(entity);
        return typeDepenseMapper.toDto(saved);
    }

    @Transactional
    public TypeDepense updateTypeDepense(Long id, TypeDepenseRequest request) {
        TypeDepenseEntity entity = typeDepenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de dépense", id));
        typeDepenseMapper.updateEntity(request, entity);
        TypeDepenseEntity saved = typeDepenseRepository.save(entity);
        return typeDepenseMapper.toDto(saved);
    }

    @Transactional
    public void deleteTypeDepense(Long id) {
        if (!typeDepenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Type de dépense", id);
        }
        typeDepenseRepository.deleteById(id);
    }
}
