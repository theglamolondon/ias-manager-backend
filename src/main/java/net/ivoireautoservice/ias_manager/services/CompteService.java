package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.core.CompteUtilisateur;
import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.CompteUtilisateurRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import net.ivoireautoservice.ias_manager.entity.CompteUtilisateurEntity;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.CompteMapper;
import net.ivoireautoservice.ias_manager.mapper.CompteUtilisateurMapper;
import net.ivoireautoservice.ias_manager.mapper.LigneCompteMapper;
import net.ivoireautoservice.ias_manager.repository.CompteRepository;
import net.ivoireautoservice.ias_manager.repository.CompteUtilisateurRepository;
import net.ivoireautoservice.ias_manager.repository.LigneCompteRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;
    private final CompteUtilisateurRepository compteUtilisateurRepository;
    private final LigneCompteRepository ligneCompteRepository;
    private final UserRepository userRepository;
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

    @Transactional
    public LigneCompte createLigne(Long compteId, LigneCompteRequest request) {
        return ligneCompteMapper.toDto(createLigneEntity(compteId, request));
    }

    @Transactional
    public LigneCompteEntity createLigneEntity(Long compteId, LigneCompteRequest request) {
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
                .dhmsOperation(LocalDateTime.now())
                .objet(request.getObjet())
                .montant(request.getMontant())
                .balanceAvant(balanceAvant)
                .observation(request.getObservation())
                .build();

        return ligneCompteRepository.save(entity);
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
