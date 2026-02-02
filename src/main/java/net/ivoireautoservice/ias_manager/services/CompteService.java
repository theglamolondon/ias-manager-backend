package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.CompteMapper;
import net.ivoireautoservice.ias_manager.mapper.LigneCompteMapper;
import net.ivoireautoservice.ias_manager.repository.CompteRepository;
import net.ivoireautoservice.ias_manager.repository.LigneCompteRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;
    private final LigneCompteRepository ligneCompteRepository;
    private final UserRepository userRepository;
    private final CompteMapper compteMapper;
    private final LigneCompteMapper ligneCompteMapper;

    // ==================== COMPTES ====================

    @Transactional(readOnly = true)
    public List<Compte> getAllComptes() {
        return compteMapper.toDtoList(compteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Compte> getAllComptes(Pageable pageable) {
        return PagedResponse.of(compteRepository.findAll(pageable)
                .map(compteMapper::toDto));
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
        CompteEntity saved = compteRepository.save(entity);
        return compteMapper.toDto(saved);
    }

    @Transactional
    public Compte updateCompte(Long id, CompteRequest request) {
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", id));
        compteMapper.updateEntity(request, entity);
        CompteEntity saved = compteRepository.save(entity);
        return compteMapper.toDto(saved);
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

        Utilisateur utilisateur = userRepository.findById(request.getUtilisateurId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.getUtilisateurId()));

        LigneCompteEntity entity = LigneCompteEntity.builder()
                .utilisateur(utilisateur)
                .compte(compte)
                .dhmsOperation(LocalDateTime.now())
                .objet(request.getObjet())
                .montant(request.getMontant())
                .balanceAvant(compte.getBalance())
                .observation(request.getObservation())
                .build();

        LigneCompteEntity saved = ligneCompteRepository.save(entity);
        return ligneCompteMapper.toDto(saved);
    }

    @Transactional
    public void deleteLigne(Long compteId, Long ligneId) {
        LigneCompteEntity entity = ligneCompteRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de compte", ligneId));
        if (!entity.getCompte().getId().equals(compteId)) {
            throw new ResourceNotFoundException("Ligne de compte " + ligneId + " non trouvée pour le compte " + compteId);
        }
        ligneCompteRepository.delete(entity);
    }
}
