package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
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
    private final CompteService compteService;

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

        // Si la date de début est aujourd'hui ou demain, démarrer directement
        if (entity.getDhmsDebut() != null) {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            if (!entity.getDhmsDebut().isAfter(tomorrow)) {
                entity.setStatut(InterventionStatut.EN_COURS);
                VehiculeEntity vehicule = entity.getVehicule();
                if (vehicule.getStatut() != VehiculeStatusEnum.MISSION) {
                    vehicule.setStatut(VehiculeStatusEnum.GARAGE);
                    vehiculeRepository.save(vehicule);
                }
            }
        }

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
        if (vehicule.getStatut() != VehiculeStatusEnum.MISSION) {
            vehicule.setStatut(VehiculeStatusEnum.GARAGE);
            vehiculeRepository.save(vehicule);
        }

        return interventionMapper.toDto(interventionRepository.save(entity));
    }

    @Transactional
    public Intervention cloturerIntervention(Long id, boolean vehiculeDisponible, Long compteId) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));

        if (entity.getStatut() != InterventionStatut.EN_COURS) {
            throw new BadRequestException("Seule une intervention au statut EN_COURS peut être clôturée");
        }

        // Si un coût est défini et un compte fourni, enregistrer la dépense
        if (entity.getCout() != null && entity.getCout() > 0 && compteId != null) {
            String vehiculeInfo = entity.getVehicule().getImmatriculation();
            String typeInfo = entity.getTypeIntervention() != null ? entity.getTypeIntervention().getLibelle() : "Intervention";
            LigneCompteRequest ligneRequest = LigneCompteRequest.builder()
                    .type(CompteLigneType.DEPENSE)
                    .montant(entity.getCout())
                    .objet("INTERVENTION " + typeInfo + " — " + vehiculeInfo)
                    .observation(entity.getObjet())
                    .build();
            compteService.createLigne(compteId, ligneRequest);
        }

        entity.setStatut(InterventionStatut.CLOTUREE);
        entity.setDhmsFin(LocalDate.now());

        VehiculeEntity vehicule = entity.getVehicule();
        if (vehicule.getStatut() != VehiculeStatusEnum.MISSION) {
            // DISPONIBLE si réparation terminée, GARAGE si d'autres travaux restent à faire
            vehicule.setStatut(vehiculeDisponible ? VehiculeStatusEnum.DISPONIBLE : VehiculeStatusEnum.GARAGE);
            vehiculeRepository.save(vehicule);
        }

        return interventionMapper.toDto(interventionRepository.save(entity));
    }

    private void resolveRelations(InterventionRequest request, InterventionEntity entity) {
        VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));
        entity.setVehicule(vehicule);

        TypeInterventionEntity typeIntervention = typeInterventionRepository.findById(request.getTypeInterventionId())
                .orElseThrow(() -> new ResourceNotFoundException("Type d'intervention", request.getTypeInterventionId()));
        entity.setTypeIntervention(typeIntervention);

        if (request.getGarageId() != null) {
            PartenaireEntity garage = partenaireRepository.findById(request.getGarageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getGarageId()));
            entity.setGarage(garage);
        } else {
            entity.setGarage(null);
        }
    }
}
