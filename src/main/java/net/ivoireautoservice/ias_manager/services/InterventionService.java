package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.InterventionMapper;
import net.ivoireautoservice.ias_manager.repository.InterventionRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.TypeInterventionRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final VehiculeRepository vehiculeRepository;
    private final TypeInterventionRepository typeInterventionRepository;
    private final PartenaireRepository partenaireRepository;
    private final InterventionMapper interventionMapper;

    @Transactional(readOnly = true)
    public PagedResponse<Intervention> getAllInterventions(String keyword, Pageable pageable) {
        Page<InterventionEntity> page = (keyword != null && !keyword.isBlank())
                ? interventionRepository.searchByKeyword(keyword.trim(), pageable)
                : interventionRepository.findAll(pageable);
        return PagedResponse.of(page.map(interventionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PagedResponse<Intervention> getInterventionsByVehicule(Long vehiculeId, Pageable pageable) {
        if (!vehiculeRepository.existsById(vehiculeId)) {
            throw new ResourceNotFoundException("Véhicule", vehiculeId);
        }
        return PagedResponse.of(interventionRepository.findByVehiculeId(vehiculeId, pageable)
                .map(interventionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Intervention getInterventionById(Long id) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
        return interventionMapper.toDto(entity);
    }

    @Transactional
    public Intervention createIntervention(InterventionRequest request) {
        InterventionEntity entity = interventionMapper.toEntity(request);
        resolveRelations(request, entity);
        InterventionEntity saved = interventionRepository.save(entity);
        return interventionMapper.toDto(saved);
    }

    @Transactional
    public Intervention updateIntervention(Long id, InterventionRequest request) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
        interventionMapper.updateEntity(request, entity);
        resolveRelations(request, entity);
        InterventionEntity saved = interventionRepository.save(entity);
        return interventionMapper.toDto(saved);
    }

    @Transactional
    public void deleteIntervention(Long id) {
        if (!interventionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Intervention", id);
        }
        interventionRepository.deleteById(id);
    }

    @Transactional
    public Intervention demarrerIntervention(Long id) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));

        if (entity.getStatut() != InterventionStatut.CREEE) {
            throw new BadRequestException("Seule une intervention au statut CREEE peut être démarrée");
        }

        entity.setStatut(InterventionStatut.EN_COURS);
        entity.setDhmsDebut(LocalDate.now());

        VehiculeEntity vehicule = entity.getVehicule();
        vehicule.setStatut(VehiculeStatusEnum.PANNE);
        vehiculeRepository.save(vehicule);

        return interventionMapper.toDto(interventionRepository.save(entity));
    }

    @Transactional
    public Intervention cloturerIntervention(Long id, boolean vehiculeDisponible) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));

        if (entity.getStatut() != InterventionStatut.EN_COURS) {
            throw new BadRequestException("Seule une intervention au statut EN_COURS peut être clôturée");
        }

        entity.setStatut(InterventionStatut.CLOTUREE);
        entity.setDhmsFin(LocalDate.now());

        VehiculeEntity vehicule = entity.getVehicule();
        vehicule.setStatut(vehiculeDisponible ? VehiculeStatusEnum.DISPONIBLE : VehiculeStatusEnum.INDISPONIBLE);
        vehiculeRepository.save(vehicule);

        return interventionMapper.toDto(interventionRepository.save(entity));
    }

    private void resolveRelations(InterventionRequest request, InterventionEntity entity) {
        VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));
        entity.setVehicule(vehicule);

        TypeInterventionEntity typeIntervention = typeInterventionRepository.findById(request.getTypeInterventionId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'intervention", request.getTypeInterventionId()));
        entity.setTypeIntervention(typeIntervention);

        if (request.getFournisseurId() != null) {
            PartenaireEntity fournisseur = partenaireRepository.findById(request.getFournisseurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getFournisseurId()));
            entity.setFournisseur(fournisseur);
        } else {
            entity.setFournisseur(null);
        }
    }
}
