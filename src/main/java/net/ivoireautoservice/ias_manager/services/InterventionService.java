package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.LigneCompteOrigine;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.InterventionMapper;
import net.ivoireautoservice.ias_manager.repository.InterventionRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.TypeInterventionRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import java.time.LocalDate;
import java.util.Objects;
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

        if (entity.getStatut() == InterventionStatut.CLOTUREE) {
            throw new BadRequestException("Une intervention clôturée ne peut plus être modifiée");
        }

        // Déplacer une intervention EN_COURS vers un autre véhicule laisserait l'ancien
        // immobilisé au GARAGE sans intervention pour l'en sortir.
        Long vehiculeActuelId = entity.getVehicule() != null ? entity.getVehicule().getId() : null;
        if (entity.getStatut() == InterventionStatut.EN_COURS
                && vehiculeActuelId != null
                && !vehiculeActuelId.equals(request.getVehiculeId())) {
            throw new BadRequestException("Le véhicule d'une intervention en cours ne peut pas être changé. "
                    + "Clôturez cette intervention puis créez-en une nouvelle sur l'autre véhicule.");
        }

        // Le coût est figé une fois la dépense passée en trésorerie, sinon les écritures divergent.
        if (entity.getDhmsPaiement() != null && !Objects.equals(entity.getCout(), request.getCout())) {
            throw new BadRequestException("Le coût d'une intervention déjà payée ne peut plus être modifié");
        }

        interventionMapper.updateEntity(request, entity);
        resolveRelations(request, entity);
        InterventionEntity saved = interventionRepository.save(entity);
        return interventionMapper.toDto(saved);
    }

    @Transactional
    public void deleteIntervention(Long id) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));

        // Supprimer une intervention EN_COURS laisserait son véhicule au GARAGE sans
        // aucune trace pour l'en sortir : il faut la clôturer, ce qui rend le véhicule.
        if (entity.getStatut() == InterventionStatut.EN_COURS) {
            throw new BadRequestException("Une intervention en cours ne peut pas être supprimée. "
                    + "Clôturez-la d'abord afin de rendre le véhicule à la flotte.");
        }

        if (entity.getDhmsPaiement() != null) {
            throw new BadRequestException("Une intervention déjà payée ne peut pas être supprimée "
                    + "(la dépense correspondante est enregistrée en trésorerie)");
        }

        interventionRepository.delete(entity);
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

    /**
     * Clôture l'intervention et rend le véhicule à la flotte.
     *
     * <p>La clôture est purement opérationnelle : elle ne touche pas à la trésorerie.
     * Le règlement du garage est une action distincte ({@link #payerIntervention}) qui
     * horodate {@code dhmsPaiement}, trace le compte débité et produit l'unique
     * décaissement de l'intervention — clôturer et payer sur le même compte
     * débiterait sinon deux fois le même coût.</p>
     */
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
        if (vehicule.getStatut() != VehiculeStatusEnum.MISSION) {
            // DISPONIBLE si réparation terminée, GARAGE si d'autres travaux restent à faire
            vehicule.setStatut(vehiculeDisponible ? VehiculeStatusEnum.DISPONIBLE : VehiculeStatusEnum.GARAGE);
            vehiculeRepository.save(vehicule);
        }

        return interventionMapper.toDto(interventionRepository.save(entity));
    }

    /**
     * Mouvement de trésorerie correspondant au coût de l'intervention.
     *
     * <p>La ligne porte le véhicule — la vue trésorerie peut ainsi filtrer sur lui —
     * mais son origine {@code INTERVENTION} l'exclut des agrégats de coût du véhicule :
     * la valeur analytique reste portée par {@code interventions.cout}, dès la clôture
     * et non au décaissement.</p>
     */
    private LigneCompteRequest ligneDepense(InterventionEntity entity) {
        String vehiculeInfo = entity.getVehicule().getImmatriculation();
        String typeInfo = entity.getTypeIntervention() != null
                ? entity.getTypeIntervention().getLibelle()
                : "Intervention";
        return LigneCompteRequest.builder()
                .type(CompteLigneType.DEPENSE)
                .montant(entity.getCout())
                .objet("INTERVENTION " + typeInfo + " — " + vehiculeInfo)
                .observation(entity.getObjet())
                .vehiculeId(entity.getVehicule().getId())
                .build();
    }

    /**
     * Enregistre le règlement de l'intervention : passe la dépense sur le compte choisi
     * et horodate le paiement. Indépendant du statut de l'intervention.
     */
    @Transactional
    public Intervention payerIntervention(Long id, Long compteId) {
        InterventionEntity entity = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));

        if (entity.getDhmsPaiement() != null) {
            throw new BadRequestException("Cette intervention a déjà été payée le "
                    + entity.getDhmsPaiement());
        }
        if (entity.getCout() == null || entity.getCout() <= 0) {
            throw new BadRequestException("Renseignez le coût de l'intervention avant de la payer");
        }
        if (compteId == null) {
            throw new BadRequestException("Le compte à débiter est obligatoire");
        }

        LigneCompteEntity ligne = compteService.createLigneEntity(
                compteId, ligneDepense(entity), LigneCompteOrigine.INTERVENTION);

        entity.setDhmsPaiement(LocalDate.now());
        entity.setComptePaiement(ligne.getCompte());

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
