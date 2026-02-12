package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Vehicule;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.entity.TypeCarburantEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.TypeVehiculeEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.InterventionMapper;
import net.ivoireautoservice.ias_manager.mapper.VehiculeMapper;
import net.ivoireautoservice.ias_manager.repository.InterventionRepository;
import net.ivoireautoservice.ias_manager.repository.MediaRepository;
import net.ivoireautoservice.ias_manager.repository.TypeCarburantRepository;
import net.ivoireautoservice.ias_manager.repository.TypeInterventionRepository;
import net.ivoireautoservice.ias_manager.repository.TypeVehiculeRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final TypeVehiculeRepository typeVehiculeRepository;
    private final TypeCarburantRepository typeCarburantRepository;
    private final TypeInterventionRepository typeInterventionRepository;
    private final InterventionRepository interventionRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final SharedService sharedService;
    private final VehiculeMapper vehiculeMapper;
    private final InterventionMapper interventionMapper;

    @Transactional(readOnly = true)
    public List<Vehicule> getAllVehicules() {
        return vehiculeMapper.toDtoList(vehiculeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Vehicule> getAllVehicules(Pageable pageable) {
        Page<Vehicule> dtoPage = vehiculeRepository.findAll(pageable).map(vehiculeMapper::toDto);
        return PagedResponse.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public Vehicule getVehiculeById(Long id) {
        VehiculeEntity entity = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", id));
        return vehiculeMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Vehicule getVehiculeByImmatriculation(String immatriculation) {
        VehiculeEntity entity = vehiculeRepository.findByImmatriculation(immatriculation)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule avec immatriculation " + immatriculation + " non trouvé"));
        return vehiculeMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<Vehicule> getVehiculesByStatut(VehiculeStatusEnum statut) {
        return vehiculeMapper.toDtoList(vehiculeRepository.findByStatut(statut));
    }

    @Transactional(readOnly = true)
    public List<Vehicule> getVehiculesByType(Long typeId) {
        return vehiculeMapper.toDtoList(vehiculeRepository.findByTypeId(typeId));
    }

    @Transactional(readOnly = true)
    public List<Vehicule> getVehiculesByCategorie(Long categorieId) {
        return vehiculeMapper.toDtoList(vehiculeRepository.findByTypeCategorieId(categorieId));
    }

    @Transactional
    public Vehicule createVehicule(VehiculeRequest request) {
        TypeVehiculeEntity type = typeVehiculeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de véhicule", request.getTypeId()));

        VehiculeEntity entity = vehiculeMapper.toEntity(request);
        entity.setType(type);
        entity.setStatut(VehiculeStatusEnum.DISPONIBLE);
        resolveMarque(request, entity);
        resolveTypeCarburant(request, entity);
        resolvePhotos(request, entity);

        VehiculeEntity saved = vehiculeRepository.save(entity);
        return vehiculeMapper.toDto(saved);
    }

    @Transactional
    public Vehicule updateVehicule(Long id, VehiculeRequest request) {
        VehiculeEntity entity = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", id));

        TypeVehiculeEntity type = typeVehiculeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de véhicule", request.getTypeId()));

        vehiculeMapper.updateEntity(request, entity);
        entity.setType(type);
        resolveMarque(request, entity);
        resolveTypeCarburant(request, entity);
        resolvePhotos(request, entity);

        VehiculeEntity saved = vehiculeRepository.save(entity);
        return vehiculeMapper.toDto(saved);
    }

    @Transactional
    public Vehicule updateStatut(Long id, VehiculeStatusEnum statut) {
        VehiculeEntity entity = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", id));

        entity.setStatut(statut);
        VehiculeEntity saved = vehiculeRepository.save(entity);
        return vehiculeMapper.toDto(saved);
    }

    @Transactional
    public void deleteVehicule(Long id) {
        if (!vehiculeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Véhicule", id);
        }
        vehiculeRepository.deleteById(id);
    }


    @Transactional(readOnly = true)
    public PagedResponse<Intervention> getInterventionsByVehicule(Long vehiculeId, Pageable pageable) {
        if (!vehiculeRepository.existsById(vehiculeId)) {
            throw new ResourceNotFoundException("Véhicule", vehiculeId);
        }
        return PagedResponse.of(interventionRepository.findByVehiculeId(vehiculeId, pageable)
                .map(interventionMapper::toDto));
    }

    @Transactional
    public Intervention createIntervention(Long vehiculeId, InterventionRequest request) {
        VehiculeEntity vehicule = vehiculeRepository.findById(vehiculeId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", vehiculeId));

        TypeInterventionEntity typeIntervention = typeInterventionRepository.findById(request.getTypeInterventionId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'intervention", request.getTypeInterventionId()));

        InterventionEntity entity = interventionMapper.toEntity(request);
        entity.setVehicule(vehicule);
        entity.setTypeIntervention(typeIntervention);

        InterventionEntity saved = interventionRepository.save(entity);
        return interventionMapper.toDto(saved);
    }


    @Transactional
    public Vehicule updatePhotos(Long id,
                                 MultipartFile photoAvant,
                                 MultipartFile photoArriere,
                                 MultipartFile photoCoteDroit,
                                 MultipartFile photoCoteGauche) {
        VehiculeEntity entity = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", id));

        if (photoAvant != null && !photoAvant.isEmpty()) {
            entity.setPhotoAvant(mediaService.getMediaEntity(mediaService.uploadMedia(photoAvant).getId()));
        }
        if (photoArriere != null && !photoArriere.isEmpty()) {
            entity.setPhotoArriere(mediaService.getMediaEntity(mediaService.uploadMedia(photoArriere).getId()));
        }
        if (photoCoteDroit != null && !photoCoteDroit.isEmpty()) {
            entity.setPhotoCoteDroit(mediaService.getMediaEntity(mediaService.uploadMedia(photoCoteDroit).getId()));
        }
        if (photoCoteGauche != null && !photoCoteGauche.isEmpty()) {
            entity.setPhotoCoteGauche(mediaService.getMediaEntity(mediaService.uploadMedia(photoCoteGauche).getId()));
        }

        VehiculeEntity saved = vehiculeRepository.save(entity);
        return vehiculeMapper.toDto(saved);
    }

    private void resolveMarque(VehiculeRequest request, VehiculeEntity entity) {
        if (request.getMarque() != null && !request.getMarque().isBlank()) {
            entity.setMarque(sharedService.getOrCreateMarque(request.getMarque()));
        } else {
            entity.setMarque(null);
        }
    }

    private void resolveTypeCarburant(VehiculeRequest request, VehiculeEntity entity) {
        if (request.getTypeCarburantId() != null) {
            TypeCarburantEntity typeCarburant = typeCarburantRepository.findById(request.getTypeCarburantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type de carburant", request.getTypeCarburantId()));
            entity.setTypeCarburant(typeCarburant);
        } else {
            entity.setTypeCarburant(null);
        }
    }

    private void resolvePhotos(VehiculeRequest request, VehiculeEntity entity) {
        entity.setPhotoAvant(resolveMedia(request.getPhotoAvantId()));
        entity.setPhotoArriere(resolveMedia(request.getPhotoArriereId()));
        entity.setPhotoCoteDroit(resolveMedia(request.getPhotoCoteDroitId()));
        entity.setPhotoCoteGauche(resolveMedia(request.getPhotoCoteGaucheId()));
    }

    private MediaEntity resolveMedia(String mediaId) {
        if (mediaId == null) return null;
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Média avec l'id " + mediaId + " non trouvé"));
    }
}
