package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
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
    private final CompteMapper compteMapper;
    private final CompteUtilisateurMapper compteUtilisateurMapper;
    private final LigneCompteMapper ligneCompteMapper;

    // ==================== COMPTES ====================

    @Transactional(readOnly = true)
    public List<Compte> getAllComptes() {
        return compteMapper.toDtoList(compteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Compte> getAllComptes(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? compteRepository.searchByKeyword(keyword.trim(), pageable)
                : compteRepository.findAll(pageable);
        return PagedResponse.of(page.map(compteMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Compte getCompteById(Long id) {
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", id));
        return compteMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Compte getCompteByNumero(String numero) {
        CompteEntity entity = compteRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Compte avec numéro " + numero + " non trouvé"));
        return compteMapper.toDto(entity);
    }

    @Transactional
    public Compte createCompte(CompteRequest request) {
        CompteEntity entity = compteMapper.toEntity(request);

        // Résoudre le manager
        if (request.getManagerId() != null) {
            Utilisateur manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.getManagerId()));
            entity.setManager(manager);
        }

        CompteEntity saved = compteRepository.save(entity);

        // Gérer les utilisateurs du compte
        syncCompteUtilisateurs(saved, request);

        return compteMapper.toDto(saved);
    }

    @Transactional
    public Compte updateCompte(Long id, CompteRequest request) {
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

                CompteUtilisateurEntity cuEntity = CompteUtilisateurEntity.builder()
                        .compte(compte)
                        .utilisateur(utilisateur)
                        .canAppro(canAppro)
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

    // ==================== LIGNES COMPTE ====================

    @Transactional(readOnly = true)
    public PagedResponse<LigneCompte> getLignesByCompte(Long compteId, Pageable pageable) {
        if (!compteRepository.existsById(compteId)) {
            throw new ResourceNotFoundException("Compte", compteId);
        }
        return PagedResponse.of(ligneCompteRepository.findByCompteId(compteId, pageable)
                .map(ligneCompteMapper::toDto));
    }

    @Transactional(readOnly = true)
    public LigneCompte getLigneById(Long compteId, Long ligneId) {
        LigneCompteEntity entity = ligneCompteRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de compte", ligneId));
        if (!entity.getCompte().getId().equals(compteId)) {
            throw new ResourceNotFoundException("Ligne de compte " + ligneId + " non trouvée pour le compte " + compteId);
        }
        return ligneCompteMapper.toDto(entity);
    }

    @Transactional
    public LigneCompte createLigne(Long compteId, LigneCompteRequest request) {
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
        if (request.getType() == CompteLigneType.DEPENSE) {
            nouvelleBalance = balanceAvant - request.getMontant();
        } else {
            nouvelleBalance = balanceAvant + request.getMontant();
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

        LigneCompteEntity saved = ligneCompteRepository.save(entity);
        return ligneCompteMapper.toDto(saved);
    }

}
