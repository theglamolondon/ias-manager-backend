package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.core.CompteUtilisateur;
import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.SyntheseCompte;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.CompteUtilisateurRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneImputationRequest;
import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import net.ivoireautoservice.ias_manager.entity.CompteUtilisateurEntity;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.entity.MissionEntity;
import net.ivoireautoservice.ias_manager.entity.TypeDepenseEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.LigneCompteOrigine;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.CompteMapper;
import net.ivoireautoservice.ias_manager.mapper.CompteUtilisateurMapper;
import net.ivoireautoservice.ias_manager.mapper.LigneCompteMapper;
import net.ivoireautoservice.ias_manager.repository.CompteRepository;
import net.ivoireautoservice.ias_manager.repository.CompteUtilisateurRepository;
import net.ivoireautoservice.ias_manager.repository.LigneCompteRepository;
import net.ivoireautoservice.ias_manager.repository.MissionRepository;
import net.ivoireautoservice.ias_manager.repository.TypeDepenseRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;
    private final CompteUtilisateurRepository compteUtilisateurRepository;
    private final LigneCompteRepository ligneCompteRepository;
    private final UserRepository userRepository;
    private final TypeDepenseRepository typeDepenseRepository;
    private final VehiculeRepository vehiculeRepository;
    private final MissionRepository missionRepository;
    private final SecurityService securityService;
    private final MediaService mediaService;
    private final CompteMapper compteMapper;
    private final CompteUtilisateurMapper compteUtilisateurMapper;
    private final LigneCompteMapper ligneCompteMapper;

    // ==================== PÉRIMÈTRE DE VISIBILITÉ ====================

    /**
     * Périmètre de lecture de l'utilisateur courant : {@code null} pour un trésorier
     * en chef ({@code TRESORERIE_ADMIN}, qui voit tous les comptes), sinon son id —
     * il ne voit alors que les comptes auxquels il est rattaché. Ce seul paramètre
     * est passé tel quel aux requêtes (aucun branchement à dupliquer).
     *
     * <p>Ne concerne que la <b>lecture</b> : les mouvements (approvisionnement,
     * dépense, solde) restent conditionnés à une affectation explicite sur le
     * compte, y compris pour un trésorier en chef.</p>
     */
    private Long perimetre() {
        return securityService.hasAuthority(PermissionEnum.TRESORERIE_ADMIN)
                ? null
                : securityService.getUtilisateurConnecte().getId();
    }

    /** Charge un compte du périmètre de l'utilisateur, ou 404 (on ne divulgue pas son existence). */
    private CompteEntity getCompteVisible(Long id) {
        return compteRepository.findVisibleById(id, perimetre())
                .orElseThrow(() -> new ResourceNotFoundException("Compte", id));
    }

    // ==================== COMPTES ====================

    @Transactional(readOnly = true)
    public List<Compte> getAllComptes() {
        return compteMapper.toDtoList(compteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Compte> getAllComptes(String keyword, Pageable pageable) {
        String recherche = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return PagedResponse.of(compteRepository.search(recherche, perimetre(), pageable)
                .map(compteMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Compte getCompteById(Long id) {
        return compteMapper.toDto(getCompteVisible(id));
    }

    @Transactional(readOnly = true)
    public Compte getCompteByNumero(String numero) {
        CompteEntity entity = compteRepository.findVisibleByNumero(numero, perimetre())
                .orElseThrow(() -> new ResourceNotFoundException("Compte avec numéro " + numero + " non trouvé"));
        return compteMapper.toDto(entity);
    }

    @Transactional
    public Compte createCompte(CompteRequest request, org.springframework.web.multipart.MultipartFile logo) {
        CompteEntity entity = compteMapper.toEntity(request);

        // Le compte démarre toujours à 0 : un montant initial renseigné donne lieu
        // à une ligne d'approvisionnement (voir approvisionnementInitial plus bas)
        long montantInitial = request.getBalance() != null ? request.getBalance() : 0L;
        entity.setBalance(0L);

        // Résoudre le manager
        if (request.getManagerId() != null) {
            Utilisateur manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.getManagerId()));
            entity.setManager(manager);
        }

        if (logo != null && !logo.isEmpty()) {
            entity.setLogo(mediaService.getMediaEntity(mediaService.uploadMedia(logo).getId()));
        }

        CompteEntity saved = compteRepository.save(entity);

        // Gérer les utilisateurs du compte
        syncCompteUtilisateurs(saved, request);

        if (montantInitial != 0) {
            approvisionnementInitial(saved, montantInitial);
        }

        return compteMapper.toDto(saved);
    }

    /**
     * Trace le montant renseigné à la création sous forme d'une ligne d'approvisionnement,
     * pour que la balance du compte reste la somme de ses opérations.
     */
    private void approvisionnementInitial(CompteEntity compte, long montant) {
        if (montant < 0 && !Boolean.TRUE.equals(compte.getCanBeNegative())) {
            throw new BadRequestException("Solde insuffisant. Ce compte n'autorise pas un solde négatif");
        }

        LigneCompteEntity ligne = LigneCompteEntity.builder()
                .utilisateur(securityService.getUtilisateurConnecte())
                .compte(compte)
                .type(montant >= 0 ? CompteLigneType.APPROVISIONNEMENT : CompteLigneType.DEPENSE)
                .origine(LigneCompteOrigine.MANUELLE)
                .dhmsOperation(LocalDateTime.now())
                .objet("APPROVISIONNEMENT INITIAL")
                .montant(Math.abs(montant))
                .balanceAvant(0L)
                .build();
        ligneCompteRepository.save(ligne);

        compte.setBalance(montant);
        compteRepository.save(compte);
    }

    @Transactional
    public Compte updateCompte(Long id, CompteRequest request, org.springframework.web.multipart.MultipartFile logo) {
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", id));
        compteMapper.updateEntity(request, entity);

        // Résoudre le manager
        if (request.getManagerId() != null) {
            Utilisateur manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.getManagerId()));
            entity.setManager(manager);
        } else {
            entity.setManager(null);
        }

        if (logo != null && !logo.isEmpty()) {
            entity.setLogo(mediaService.getMediaEntity(mediaService.uploadMedia(logo).getId()));
        }

        CompteEntity saved = compteRepository.save(entity);

        // Gérer les utilisateurs du compte
        syncCompteUtilisateurs(saved, request);

        return compteMapper.toDto(saved);
    }

    private void syncCompteUtilisateurs(CompteEntity compte, CompteRequest request) {
        // Supprimer les anciens
        compte.getUtilisateurs().clear();
        compteRepository.flush();

        if (request.getUtilisateurs() != null && !request.getUtilisateurs().isEmpty()) {
            for (CompteUtilisateurRequest cuReq : request.getUtilisateurs()) {
                Utilisateur utilisateur = userRepository.findById(cuReq.getUtilisateurId())
                        .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", cuReq.getUtilisateurId()));

                // Si canAppro du compte est false, forcer canAppro utilisateur à false
                boolean canAppro = Boolean.TRUE.equals(cuReq.getCanAppro())
                        && Boolean.TRUE.equals(compte.getCanAppro());

                boolean canSettle = Boolean.TRUE.equals(cuReq.getCanSettle());

                CompteUtilisateurEntity cuEntity = CompteUtilisateurEntity.builder()
                        .compte(compte)
                        .utilisateur(utilisateur)
                        .canAppro(canAppro)
                        .canSettle(canSettle)
                        .build();

                compte.getUtilisateurs().add(cuEntity);
            }
        }

        compteRepository.save(compte);
    }

    @Transactional
    public void deleteCompte(Long id) {
        if (!compteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Compte", id);
        }
        compteRepository.deleteById(id);
    }

    // ==================== MES COMPTES ====================

    @Transactional(readOnly = true)
    public List<Compte> getMesComptes(boolean factureClient) {
        Utilisateur utilisateur = securityService.getUtilisateurConnecte();

        List<CompteUtilisateurEntity> associations;
        if (factureClient) {
            // Encaissement : uniquement les comptes avec canAppro sur le compte ET sur l'utilisateur
            associations = compteUtilisateurRepository
                    .findByUtilisateurIdAndCanApproTrueAndCompteCanApproTrue(utilisateur.getId());
        } else {
            // Décaissement : tous les comptes attribués à l'utilisateur
            associations = compteUtilisateurRepository.findByUtilisateurId(utilisateur.getId());
        }

        return associations.stream()
                .map(cu -> compteMapper.toDto(cu.getCompte()))
                .toList();
    }

    // ==================== LIGNES COMPTE ====================

    @Transactional(readOnly = true)
    public PagedResponse<LigneCompte> getLignesByCompte(Long compteId, Pageable pageable) {
        getCompteVisible(compteId);
        return PagedResponse.of(ligneCompteRepository.findByCompteId(compteId, pageable)
                .map(ligneCompteMapper::toDto));
    }

    /**
     * Totaux de la fiche compte. Ils sont agrégés en base sur toutes les opérations :
     * les déduire des lignes renvoyées par {@link #getLignesByCompte} ne totaliserait
     * que la page consultée.
     */
    @Transactional(readOnly = true)
    public SyntheseCompte getSyntheseCompte(Long compteId) {
        getCompteVisible(compteId);

        Map<CompteLigneType, Long> montants = new EnumMap<>(CompteLigneType.class);
        long nombreOperations = 0L;
        for (Object[] ligne : ligneCompteRepository.agregerParTypeByCompte(compteId)) {
            if (ligne[0] == null) {
                continue;
            }
            montants.put((CompteLigneType) ligne[0], ((Number) ligne[1]).longValue());
            nombreOperations += ((Number) ligne[2]).longValue();
        }

        long remboursements = montants.getOrDefault(CompteLigneType.REMBOURSEMENT, 0L);
        return SyntheseCompte.builder()
                // Dépenses et remboursements débitent tous deux le compte
                .totalDepenses(montants.getOrDefault(CompteLigneType.DEPENSE, 0L) + remboursements)
                .totalApprovisionnements(montants.getOrDefault(CompteLigneType.APPROVISIONNEMENT, 0L))
                .totalRemboursements(remboursements)
                .totalSoldes(montants.getOrDefault(CompteLigneType.SOLDE, 0L))
                .nombreOperations(nombreOperations)
                .build();
    }

    @Transactional(readOnly = true)
    public LigneCompte getLigneById(Long compteId, Long ligneId) {
        getCompteVisible(compteId);
        LigneCompteEntity entity = ligneCompteRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de compte", ligneId));
        if (!entity.getCompte().getId().equals(compteId)) {
            throw new ResourceNotFoundException("Ligne de compte " + ligneId + " non trouvée pour le compte " + compteId);
        }
        return ligneCompteMapper.toDto(entity);
    }

    /**
     * Saisie manuelle d'un mouvement depuis l'écran de trésorerie. Contrôle l'imputation
     * analytique (voir {@link #validerSaisieManuelle}) puis enregistre le mouvement en
     * {@link LigneCompteOrigine#MANUELLE} : la ligne porte alors la valeur de la dépense
     * pour les agrégats métier.
     */
    @Transactional
    public LigneCompte createLigne(Long compteId, LigneCompteRequest request) {
        validerSaisieManuelle(request);
        return ligneCompteMapper.toDto(enregistrerMouvement(compteId, request, LigneCompteOrigine.MANUELLE));
    }

    /**
     * Mouvement généré par un autre module (règlement d'intervention, encaissement ou
     * remboursement de facture). L'origine est fournie par l'appelant et la ligne ne
     * porte alors <b>pas</b> la valeur analytique de la dépense : celle-ci reste sur
     * l'objet source, ce qui évite tout double comptage.
     */
    @Transactional
    public LigneCompteEntity createLigneEntity(Long compteId, LigneCompteRequest request,
                                               LigneCompteOrigine origine) {
        return enregistrerMouvement(compteId, request, origine);
    }

    private LigneCompteEntity enregistrerMouvement(Long compteId, LigneCompteRequest request,
                                                   LigneCompteOrigine origine) {
        CompteEntity compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", compteId));

        Utilisateur utilisateur = securityService.getUtilisateurConnecte();

        // Vérifier que l'utilisateur est autorisé sur ce compte
        CompteUtilisateurEntity compteUtilisateur = compteUtilisateurRepository
                .findByCompteIdAndUtilisateurId(compteId, utilisateur.getId())
                .orElseThrow(() -> new BadRequestException("Vous n'êtes pas autorisé à effectuer des mouvements sur ce compte"));

        // APPROVISIONNEMENT uniquement si canAppro du compte ET de l'utilisateur sont true
        if (request.getType() == CompteLigneType.APPROVISIONNEMENT) {
            if (!Boolean.TRUE.equals(compte.getCanAppro())) {
                throw new BadRequestException("Ce compte n'autorise pas l'approvisionnement");
            }
            if (!Boolean.TRUE.equals(compteUtilisateur.getCanAppro())) {
                throw new BadRequestException("Vous n'êtes pas autorisé à approvisionner ce compte");
            }
        }

        // Mise à jour de la balance selon le type
        Long balanceAvant = compte.getBalance();
        long nouvelleBalance;
        if (request.getType() == CompteLigneType.DEPENSE
                || request.getType() == CompteLigneType.REMBOURSEMENT) {
            nouvelleBalance = balanceAvant - request.getMontant();
        } else {
            nouvelleBalance = balanceAvant + request.getMontant();
        }

        // Bloquer si la balance deviendrait négative et que le compte ne l'autorise pas
        if (!Boolean.TRUE.equals(compte.getCanBeNegative()) && nouvelleBalance < 0) {
            throw new BadRequestException("Solde insuffisant. Ce compte n'autorise pas un solde négatif");
        }

        compte.setBalance(nouvelleBalance);
        compteRepository.save(compte);

        LigneCompteEntity entity = LigneCompteEntity.builder()
                .utilisateur(utilisateur)
                .compte(compte)
                .type(request.getType())
                .origine(origine)
                .dhmsOperation(LocalDateTime.now())
                .objet(request.getObjet())
                .montant(request.getMontant())
                .balanceAvant(balanceAvant)
                .observation(request.getObservation())
                .build();

        resoudreImputation(request, entity);

        return ligneCompteRepository.save(entity);
    }

    // ==================== IMPUTATION ANALYTIQUE ====================

    /**
     * Contrôle une saisie manuelle avant enregistrement.
     *
     * <p>Deux principes : l'imputation n'a de sens que sur une dépense (un
     * approvisionnement alimente le compte, il ne se rattache à aucun véhicule), et
     * elle est <b>explicite</b> — ne rien imputer se demande en cochant « non
     * imputable », faute de quoi l'oubli passerait inaperçu et la dépense
     * disparaîtrait de toutes les fiches véhicule sans que rien ne le signale.</p>
     */
    private void validerSaisieManuelle(LigneCompteRequest request) {
        if (request.getType() != CompteLigneType.DEPENSE
                && request.getType() != CompteLigneType.APPROVISIONNEMENT) {
            throw new BadRequestException("Seuls une dépense ou un approvisionnement peuvent être saisis. "
                    + "Un solde s'obtient par l'action « Solder », un remboursement par l'annulation de la facture");
        }

        boolean vehicule = request.getVehiculeId() != null;
        boolean mission = request.getMissionId() != null;
        boolean nonImputable = Boolean.TRUE.equals(request.getNonImputable());

        if (request.getType() != CompteLigneType.DEPENSE) {
            if (request.getTypeDepenseId() != null || vehicule || mission || nonImputable) {
                throw new BadRequestException("Seule une dépense peut être imputée à un véhicule ou à une mission");
            }
            return;
        }

        if (request.getTypeDepenseId() == null) {
            throw new BadRequestException("La nature de la dépense est obligatoire");
        }

        int branches = (vehicule ? 1 : 0) + (mission ? 1 : 0) + (nonImputable ? 1 : 0);
        if (branches == 0) {
            throw new BadRequestException("Précisez l'imputation de la dépense (véhicule ou mission), "
                    + "ou cochez « Dépense non imputable »");
        }
        if (branches > 1) {
            throw new BadRequestException("Une dépense s'impute à un véhicule OU à une mission, pas aux deux");
        }
    }

    /**
     * Résout les références d'imputation et les pose sur la ligne.
     *
     * <p>Le véhicule est <b>toujours</b> écrit, y compris lorsque la saisie s'est faite
     * par la mission : le véhicule d'une mission peut changer en cours de route
     * ({@code changerVehicule}), et une résolution à la lecture reporterait alors des
     * frais déjà engagés sur le véhicule suivant.</p>
     */
    private void resoudreImputation(LigneCompteRequest request, LigneCompteEntity entity) {
        if (request.getTypeDepenseId() != null) {
            TypeDepenseEntity typeDepense = typeDepenseRepository.findById(request.getTypeDepenseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type de dépense", request.getTypeDepenseId()));
            entity.setTypeDepense(typeDepense);
        }

        if (request.getMissionId() != null) {
            MissionEntity mission = missionRepository.findById(request.getMissionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mission", request.getMissionId()));
            if (mission.getVehicule() == null) {
                throw new BadRequestException("La mission " + mission.getCodeMission()
                        + " n'a pas de véhicule : imputez la dépense directement à un véhicule");
            }
            entity.setMission(mission);
            entity.setVehicule(mission.getVehicule());
        } else if (request.getVehiculeId() != null) {
            VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));
            entity.setVehicule(vehicule);
        }
    }

    /**
     * Corrige l'imputation d'une ligne existante : nature de la dépense et axe
     * analytique, jamais le montant, le compte ni le type. La balance du compte et les
     * {@code balanceAvant} des lignes suivantes restent donc intactes — une erreur de
     * montant, elle, relève d'une contre-passation et non de cette correction.
     */
    @Transactional
    public LigneCompte updateImputation(Long compteId, Long ligneId, LigneImputationRequest request) {
        getCompteVisible(compteId);

        LigneCompteEntity entity = ligneCompteRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de compte", ligneId));

        if (!entity.getCompte().getId().equals(compteId)) {
            throw new ResourceNotFoundException("Ligne de compte " + ligneId
                    + " non trouvée pour le compte " + compteId);
        }
        if (entity.getOrigine() != LigneCompteOrigine.MANUELLE) {
            throw new BadRequestException("Ce mouvement a été généré automatiquement : "
                    + "corrigez l'objet dont il provient plutôt que la ligne de trésorerie");
        }
        if (entity.getType() != CompteLigneType.DEPENSE) {
            throw new BadRequestException("Seule une dépense peut être imputée");
        }

        LigneCompteRequest imputation = LigneCompteRequest.builder()
                .type(entity.getType())
                .montant(entity.getMontant())
                .typeDepenseId(request.getTypeDepenseId())
                .vehiculeId(request.getVehiculeId())
                .missionId(request.getMissionId())
                .nonImputable(request.getNonImputable())
                .build();
        validerSaisieManuelle(imputation);

        entity.setTypeDepense(null);
        entity.setVehicule(null);
        entity.setMission(null);
        resoudreImputation(imputation, entity);

        return ligneCompteMapper.toDto(ligneCompteRepository.save(entity));
    }

    @Transactional
    public LigneCompte solderCompte(Long compteId) {
        CompteEntity compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", compteId));

        Utilisateur utilisateur = securityService.getUtilisateurConnecte();

        // Vérifier que l'utilisateur est autorisé à solder ce compte
        CompteUtilisateurEntity compteUtilisateur = compteUtilisateurRepository
                .findByCompteIdAndUtilisateurId(compteId, utilisateur.getId())
                .orElseThrow(() -> new BadRequestException("Vous n'êtes pas autorisé à effectuer des mouvements sur ce compte"));

        if (!Boolean.TRUE.equals(compteUtilisateur.getCanSettle())) {
            throw new BadRequestException("Vous n'êtes pas autorisé à solder ce compte");
        }

        Long balanceAvant = compte.getBalance();
        if (balanceAvant == 0) {
            throw new BadRequestException("Le compte est déjà soldé");
        }

        LigneCompteEntity entity = LigneCompteEntity.builder()
                .utilisateur(utilisateur)
                .compte(compte)
                .type(CompteLigneType.SOLDE)
                .origine(LigneCompteOrigine.MANUELLE)
                .dhmsOperation(LocalDateTime.now())
                .objet("SOLDE DU COMPTE")
                .montant(Math.abs(balanceAvant))
                .balanceAvant(balanceAvant)
                .build();

        compte.setBalance(0L);
        compteRepository.save(compte);

        LigneCompteEntity saved = ligneCompteRepository.save(entity);
        return ligneCompteMapper.toDto(saved);
    }

}
