package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.DepenseImputee;
import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.core.InterventionHistorique;
import net.ivoireautoservice.ias_manager.dto.core.MissionHistorique;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Vehicule;
import net.ivoireautoservice.ias_manager.dto.core.VehiculeHistorique;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.entity.AssuranceEntity;
import net.ivoireautoservice.ias_manager.entity.DocumentVehiculeEntity;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.entity.LigneFactureEntity;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.entity.MissionEntity;
import net.ivoireautoservice.ias_manager.entity.TypeAssuranceEntity;
import net.ivoireautoservice.ias_manager.entity.TypeCarburantEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.TypeVehiculeEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.LigneCompteOrigine;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.dto.core.DocumentVehicule;
import net.ivoireautoservice.ias_manager.mapper.DocumentVehiculeMapper;
import net.ivoireautoservice.ias_manager.mapper.InterventionMapper;
import net.ivoireautoservice.ias_manager.mapper.VehiculeMapper;
import net.ivoireautoservice.ias_manager.repository.LigneCompteRepository;
import net.ivoireautoservice.ias_manager.repository.DocumentVehiculeRepository;
import net.ivoireautoservice.ias_manager.repository.InterventionRepository;
import net.ivoireautoservice.ias_manager.entity.MarqueEntity;
import net.ivoireautoservice.ias_manager.repository.AssuranceRepository;
import net.ivoireautoservice.ias_manager.repository.LigneFactureRepository;
import net.ivoireautoservice.ias_manager.repository.MarqueRepository;
import net.ivoireautoservice.ias_manager.repository.MediaRepository;
import net.ivoireautoservice.ias_manager.repository.MissionRepository;
import net.ivoireautoservice.ias_manager.repository.TypeAssuranceRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final TypeVehiculeRepository typeVehiculeRepository;
    private final TypeCarburantRepository typeCarburantRepository;
    private final TypeAssuranceRepository typeAssuranceRepository;
    private final AssuranceRepository assuranceRepository;
    private final TypeInterventionRepository typeInterventionRepository;
    private final InterventionRepository interventionRepository;
    private final MissionRepository missionRepository;
    private final LigneCompteRepository ligneCompteRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final DocumentVehiculeRepository documentVehiculeRepository;
    private final MediaRepository mediaRepository;
    private final MarqueRepository marqueRepository;
    private final MediaService mediaService;
    private final SharedService sharedService;
    private final VehiculeMapper vehiculeMapper;
    private final InterventionMapper interventionMapper;
    private final DocumentVehiculeMapper documentVehiculeMapper;

    @Transactional(readOnly = true)
    public List<Vehicule> getAllVehicules() {
        return vehiculeMapper.toDtoList(vehiculeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Vehicule> getAllVehicules(String keyword, VehiculeStatusEnum statut, Long typeId, Long assuranceId, Pageable pageable) {
        Page<VehiculeEntity> page = vehiculeRepository.searchWithFilters(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                statut, typeId, assuranceId, pageable);
        return PagedResponse.of(page.map(vehiculeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Vehicule getVehiculeById(Long id) {
        VehiculeEntity entity = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", id));
        return vehiculeMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Vehicule getVehiculeByNumChassis(String numChassis) {
        VehiculeEntity entity = vehiculeRepository.findByNumChassis(numChassis)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule avec numéro de chassis " + numChassis + " non trouvé"));
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
        resolveEnergie(request, entity);
        resolveTypeAssurance(request, entity);
        resolveAssurance(request, entity);
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
        resolveEnergie(request, entity);
        resolveTypeAssurance(request, entity);
        resolveAssurance(request, entity);
        resolvePhotos(request, entity);

        VehiculeEntity saved = vehiculeRepository.save(entity);
        return vehiculeMapper.toDto(saved);
    }

    /**
     * Changement de statut manuel d'un véhicule.
     *
     * <p>C'est la soupape qui permet de sortir un véhicule d'un statut où aucun workflow
     * ne viendrait le chercher (immobilisé au GARAGE après une intervention clôturée,
     * SINISTRE, REFORME, INDISPONIBLE, ou bloqué en MISSION suite à une désynchronisation).
     * Deux garde-fous seulement, pour ne pas désaligner le statut d'un véhicule réellement
     * engagé sur une mission ou une intervention en cours.</p>
     */
    @Transactional
    public Vehicule updateStatut(Long id, VehiculeStatusEnum statut) {
        VehiculeEntity entity = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", id));

        if (statut == null) {
            throw new BadRequestException("Le statut est obligatoire");
        }
        if (entity.getStatut() == statut) {
            return vehiculeMapper.toDto(entity);
        }

        // MISSION n'est jamais posé à la main : il découle du démarrage d'une mission.
        if (statut == VehiculeStatusEnum.MISSION) {
            throw new BadRequestException("Le statut MISSION ne se pose pas manuellement : "
                    + "il est appliqué au démarrage d'une mission.");
        }

        // Un véhicule réellement engagé sur une mission ne peut pas en être sorti à la main.
        // En revanche s'il est en MISSION sans mission active (donnée orpheline), on débloque.
        if (entity.getStatut() == VehiculeStatusEnum.MISSION
                && missionRepository.existsMissionEnCoursPourVehicule(id)) {
            throw new BadRequestException("Ce véhicule est engagé sur une mission en cours. "
                    + "Terminez la mission pour libérer le véhicule.");
        }

        // Le déclarer disponible alors qu'il est encore au garage contredirait l'intervention.
        if (statut == VehiculeStatusEnum.DISPONIBLE
                && interventionRepository.existsByVehiculeIdAndStatut(id, InterventionStatut.EN_COURS)) {
            throw new BadRequestException("Ce véhicule a une intervention en cours. "
                    + "Clôturez l'intervention pour le rendre disponible.");
        }

        entity.setStatut(statut);
        VehiculeEntity saved = vehiculeRepository.save(entity);
        return vehiculeMapper.toDto(saved);
    }

    @Transactional
    public void deleteVehicule(Long id) {
        if (!vehiculeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Véhicule", id);
        }
        // Supprimer un véhicule auquel des dépenses sont imputées effacerait leur
        // rattachement : elles deviendraient des décaissements sans propriétaire.
        long depenses = ligneCompteRepository.countByVehiculeId(id);
        if (depenses > 0) {
            throw new BadRequestException("Ce véhicule porte " + depenses
                    + " opération(s) de trésorerie et ne peut pas être supprimé. "
                    + "Passez-le au statut RÉFORMÉ pour le sortir de la flotte");
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

    // ==================== DOCUMENTS ====================

    @Transactional(readOnly = true)
    public List<DocumentVehicule> getDocuments(Long vehiculeId) {
        if (!vehiculeRepository.existsById(vehiculeId)) {
            throw new ResourceNotFoundException("Véhicule", vehiculeId);
        }
        return documentVehiculeMapper.toDtoList(
                documentVehiculeRepository.findByVehiculeIdOrderByCreatedAtDesc(vehiculeId));
    }

    @Transactional
    public DocumentVehicule addDocument(Long vehiculeId, String label, MultipartFile file) {
        VehiculeEntity vehicule = vehiculeRepository.findById(vehiculeId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", vehiculeId));

        MediaEntity media = mediaService.getMediaEntity(mediaService.uploadDocument(file).getId());

        DocumentVehiculeEntity document = DocumentVehiculeEntity.builder()
                .label(label)
                .vehicule(vehicule)
                .media(media)
                .build();

        return documentVehiculeMapper.toDto(documentVehiculeRepository.save(document));
    }

    @Transactional
    public void deleteDocument(Long vehiculeId, Long documentId) {
        DocumentVehiculeEntity document = documentVehiculeRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (!document.getVehicule().getId().equals(vehiculeId)) {
            throw new ResourceNotFoundException("Document " + documentId + " n'appartient pas au véhicule " + vehiculeId);
        }

        String mediaId = document.getMedia().getId();
        documentVehiculeRepository.delete(document);
        mediaService.deleteMedia(mediaId);
    }

    /**
     * Interventions du véhicule au coût renseigné mais non réglées. Alimente
     * l'avertissement affiché à la saisie d'une dépense imputée à ce véhicule.
     */
    @Transactional(readOnly = true)
    public List<Intervention> getInterventionsNonReglees(Long vehiculeId) {
        if (!vehiculeRepository.existsById(vehiculeId)) {
            throw new ResourceNotFoundException("Véhicule", vehiculeId);
        }
        return interventionMapper.toDtoList(
                interventionRepository
                        .findByVehiculeIdAndDhmsPaiementIsNullAndCoutGreaterThanOrderByDhmsDebutDesc(vehiculeId, 0L));
    }

    // ==================== HISTORIQUE ====================

    @Transactional(readOnly = true)
    public VehiculeHistorique getHistorique(String numChassis) {
        VehiculeEntity vehiculeEntity = vehiculeRepository.findByNumChassis(numChassis)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule avec numéro de chassis " + numChassis + " non trouvé"));

        Vehicule vehicule = vehiculeMapper.toDto(vehiculeEntity);
        Long vehiculeId = vehiculeEntity.getId();

        // 1. Missions du véhicule
        List<MissionEntity> missionEntities = missionRepository.findByVehiculeIdOrderByDhmsDebutPreviDesc(vehiculeId);

        // 2. Récupérer les codeMission pour trouver les factures liées
        List<String> codeMissions = missionEntities.stream()
                .map(MissionEntity::getCodeMission)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        // 3. Trouver les lignes facture liées via extraRef (restreintes aux
        // factures de type MISSION pour éviter les faux positifs avec les
        // extraRef de lignes de factures fournisseur).
        // On conserve toutes les lignes facture par codeMission.
        Map<String, List<LigneFactureEntity>> lignesByCodeMission = new java.util.HashMap<>();
        if (!codeMissions.isEmpty()) {
            List<LigneFactureEntity> lignesFacture = ligneFactureRepository.findByExtraRefInForMission(codeMissions);
            for (LigneFactureEntity lf : lignesFacture) {
                lignesByCodeMission.computeIfAbsent(lf.getExtraRef(), k -> new java.util.ArrayList<>()).add(lf);
            }
        }

        // 4. Dépenses de trésorerie portant la valeur analytique du véhicule : une seule
        // requête, répartie en mémoire entre les missions et le hors-mission. Les lignes
        // générées par un règlement d'intervention ou une facture en sont exclues — leur
        // coût est déjà porté par l'objet source.
        List<LigneCompteEntity> lignesImputees = ligneCompteRepository.findDepensesImputees(
                vehiculeId, CompteLigneType.DEPENSE, LigneCompteOrigine.MANUELLE);

        Map<Long, List<DepenseImputee>> depensesByMission = new java.util.HashMap<>();
        List<DepenseImputee> depensesDirectes = new java.util.ArrayList<>();
        long totalDepensesDirectes = 0;

        for (LigneCompteEntity ligne : lignesImputees) {
            DepenseImputee depense = toDepenseImputee(ligne);
            totalDepensesDirectes += depense.getMontant();
            if (ligne.getMission() != null) {
                depensesByMission.computeIfAbsent(ligne.getMission().getId(), k -> new java.util.ArrayList<>())
                        .add(depense);
            } else {
                depensesDirectes.add(depense);
            }
        }

        // 5. Construire les MissionHistorique
        long totalGains = 0;
        List<MissionHistorique> missionsHistorique = new java.util.ArrayList<>();

        for (MissionEntity m : missionEntities) {
            // Dépenses imputées à la mission. Contrairement aux recettes, elles sont
            // conservées même sur une mission annulée : l'argent est sorti de caisse,
            // l'annulation de la mission ne le fait pas revenir.
            List<DepenseImputee> depenses = depensesByMission.getOrDefault(m.getId(), List.of());
            long totalDepensesMission = depenses.stream().mapToLong(DepenseImputee::getMontant).sum();
            boolean missionAnnulee = m.getDhmsAnnulation() != null;

            // Facture liée
            List<LigneFactureEntity> lignesMission = lignesByCodeMission.getOrDefault(m.getCodeMission(), List.of());
            Long factureId = null;
            String numFacture = null;
            net.ivoireautoservice.ias_manager.enums.FactureStatusEnum factureStatut = null;
            Long montantFactureTtc = null;

            if (!lignesMission.isEmpty() && lignesMission.get(0).getFacture() != null) {
                var facture = lignesMission.get(0).getFacture();
                factureId = facture.getId();
                numFacture = facture.getNumFacture() != null ? facture.getNumFacture() : facture.getNumProforma();
                factureStatut = facture.getStatut();

                // Somme du HT des lignes propres à cette mission,
                // puis application de la TVA de la facture — évite de compter le TTC
                // total d'une facture groupée multi-véhicules.
                long missionHt = lignesMission.stream()
                        .mapToLong(l -> l.getMontantHt() != null ? l.getMontantHt() : 0L)
                        .sum();
                float tva = facture.getTva() != null ? facture.getTva() : 0f;
                montantFactureTtc = missionHt + Math.round(missionHt * tva / 100f);

                if (!missionAnnulee && facture.getStatut() == net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PAYEE) {
                    totalGains += montantFactureTtc;
                }
            }

            String clientNom = m.getClient() != null ? m.getClient().getRaisonSociale() : null;
            String chauffeurNom = null;
            if (m.getChauffeur() != null && m.getChauffeur().getEmploye() != null) {
                var emp = m.getChauffeur().getEmploye();
                chauffeurNom = (emp.getNom() != null ? emp.getNom() : "") + " " + (emp.getPrenoms() != null ? emp.getPrenoms() : "");
                chauffeurNom = chauffeurNom.trim();
            }

            missionsHistorique.add(MissionHistorique.builder()
                    .id(m.getId())
                    .codeMission(m.getCodeMission())
                    .destination(m.getDestination())
                    .typeTarification(m.getTypeTarification())
                    .dhmsDebutPrevi(m.getDhmsDebutPrevi())
                    .dhmsFinPrevi(m.getDhmsFinPrevi())
                    .dhmsDebutReel(m.getDhmsDebutReel())
                    .dhmsFinReel(m.getDhmsFinReel())
                    .dureeLocation(m.getDureeLocation())
                    .montantTotalHT(m.getMontantTotalHT())
                    .clientNom(clientNom)
                    .chauffeurNom(chauffeurNom)
                    .annulee(missionAnnulee)
                    .totalDepenses(totalDepensesMission)
                    .depenses(depenses)
                    .factureId(factureId)
                    .numFacture(numFacture)
                    .factureStatut(factureStatut)
                    .montantFactureTtc(montantFactureTtc)
                    .build());
        }

        // 6. Interventions du véhicule
        List<InterventionEntity> interventionEntities = interventionRepository.findByVehiculeIdOrderByDhmsDebutDesc(vehiculeId);
        long totalDepensesInterventions = 0;

        List<InterventionHistorique> interventionsHistorique = new java.util.ArrayList<>();
        for (InterventionEntity i : interventionEntities) {
            long cout = i.getCout() != null ? i.getCout() : 0;
            totalDepensesInterventions += cout;

            interventionsHistorique.add(InterventionHistorique.builder()
                    .id(i.getId())
                    .objet(i.getObjet())
                    .details(i.getDetails())
                    .dhmsDebut(i.getDhmsDebut())
                    .dhmsFin(i.getDhmsFin())
                    .cout(i.getCout())
                    .statut(i.getStatut())
                    .typeIntervention(i.getTypeIntervention() != null ? i.getTypeIntervention().getLibelle() : null)
                    .garageNom(i.getGarage() != null ? i.getGarage().getRaisonSociale() : null)
                    .build());
        }

        // 7. Totaux. « Engagé » compte chaque dépense une fois, à la charge de son
        // porteur ; « décaissé » lit les sorties de caisse réelles, toutes origines
        // confondues. L'écart entre les deux est ce qui reste dû sur le véhicule.
        long totalDepensesEngagees = totalDepensesDirectes + totalDepensesInterventions;
        long totalDepensesDecaissees = ligneCompteRepository.sumMontantByVehicule(
                vehiculeId, CompteLigneType.DEPENSE);

        return VehiculeHistorique.builder()
                .vehicule(vehicule)
                .totalGains(totalGains)
                .totalDepenses(totalDepensesEngagees)
                .totalDepensesDirectes(totalDepensesDirectes)
                .totalDepensesInterventions(totalDepensesInterventions)
                .totalDepensesDecaissees(totalDepensesDecaissees)
                .resteAPayer(totalDepensesEngagees - totalDepensesDecaissees)
                .solde(totalGains - totalDepensesEngagees)
                .missions(missionsHistorique)
                .interventions(interventionsHistorique)
                .depensesDirectes(depensesDirectes)
                .build();
    }

    private DepenseImputee toDepenseImputee(LigneCompteEntity ligne) {
        return DepenseImputee.builder()
                .id(ligne.getId())
                .libelle(ligne.getObjet())
                .montant(ligne.getMontant() != null ? ligne.getMontant() : 0L)
                .dhmsOperation(ligne.getDhmsOperation())
                .typeDepenseId(ligne.getTypeDepense() != null ? ligne.getTypeDepense().getId() : null)
                .typeDepenseLibelle(ligne.getTypeDepense() != null ? ligne.getTypeDepense().getLibelle() : null)
                .compteId(ligne.getCompte() != null ? ligne.getCompte().getId() : null)
                .compteIntitule(ligne.getCompte() != null ? ligne.getCompte().getIntitule() : null)
                .missionId(ligne.getMission() != null ? ligne.getMission().getId() : null)
                .codeMission(ligne.getMission() != null ? ligne.getMission().getCodeMission() : null)
                .build();
    }

    private void resolveMarque(VehiculeRequest request, VehiculeEntity entity) {
        if (request.getMarqueId() != null) {
            MarqueEntity marque = marqueRepository.findById(request.getMarqueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Marque", request.getMarqueId()));
            entity.setMarque(marque);
        } else if (request.getMarque() != null && !request.getMarque().isBlank()) {
            entity.setMarque(sharedService.getOrCreateMarque(request.getMarque()));
        } else {
            entity.setMarque(null);
        }
    }

    private void resolveEnergie(VehiculeRequest request, VehiculeEntity entity) {
        if (request.getEnergieId() != null) {
            TypeCarburantEntity energie = typeCarburantRepository.findById(request.getEnergieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Énergie", request.getEnergieId()));
            entity.setEnergie(energie);
        } else {
            entity.setEnergie(null);
        }
    }

    private void resolveTypeAssurance(VehiculeRequest request, VehiculeEntity entity) {
        if (request.getTypeAssuranceId() != null) {
            TypeAssuranceEntity typeAssurance = typeAssuranceRepository.findById(request.getTypeAssuranceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type d'assurance", request.getTypeAssuranceId()));
            entity.setTypeAssurance(typeAssurance);
        } else {
            entity.setTypeAssurance(null);
        }
    }

    private void resolveAssurance(VehiculeRequest request, VehiculeEntity entity) {
        if (request.getAssuranceId() != null) {
            AssuranceEntity assurance = assuranceRepository.findById(request.getAssuranceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assurance", request.getAssuranceId()));
            entity.setAssurance(assurance);
        } else {
            entity.setAssurance(null);
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
