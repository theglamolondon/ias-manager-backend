package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Partenaire;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.dto.request.PartenaireRequest;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.BonCommandeMapper;
import net.ivoireautoservice.ias_manager.mapper.PartenaireMapper;
import net.ivoireautoservice.ias_manager.repository.BonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartenaireService {

    private final PartenaireRepository partenaireRepository;
    private final BonCommandeRepository bonCommandeRepository;
    private final PartenaireMapper partenaireMapper;
    private final BonCommandeMapper bonCommandeMapper;

    // ==================== PARTENAIRES ====================

    @Transactional(readOnly = true)
    public PagedResponse<Partenaire> getAllPartenaires(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? partenaireRepository.searchByKeyword(keyword.trim(), pageable)
                : partenaireRepository.findAll(pageable);
        return PagedResponse.of(page.map(partenaireMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Partenaire getPartenaireById(Long id) {
        PartenaireEntity entity = partenaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partenaire", id));
        return partenaireMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<Partenaire> getClients(Pageable pageable) {
        return PagedResponse.of(partenaireRepository.findByIsClientTrue(pageable).map(partenaireMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PagedResponse<Partenaire> rechercherClients(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? partenaireRepository.searchClientsByKeyword(keyword.trim(), pageable)
                : partenaireRepository.findByIsClientTrue(pageable);
        return PagedResponse.of(page.map(partenaireMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PagedResponse<Partenaire> getFournisseurs(Pageable pageable) {
        return PagedResponse.of(partenaireRepository.findByIsFournisseurTrue(pageable).map(partenaireMapper::toDto));
    }

    @Transactional
    public Partenaire createPartenaire(PartenaireRequest request) {
        PartenaireEntity entity = partenaireMapper.toEntity(request);
        PartenaireEntity saved = partenaireRepository.save(entity);
        return partenaireMapper.toDto(saved);
    }

    @Transactional
    public Partenaire updatePartenaire(Long id, PartenaireRequest request) {
        PartenaireEntity entity = partenaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partenaire", id));
        partenaireMapper.updateEntity(request, entity);
        PartenaireEntity saved = partenaireRepository.save(entity);
        return partenaireMapper.toDto(saved);
    }

    @Transactional
    public void deletePartenaire(Long id) {
        if (!partenaireRepository.existsById(id)) {
            throw new ResourceNotFoundException("Partenaire", id);
        }
        partenaireRepository.deleteById(id);
    }

    // ==================== BONS COMMANDE ====================

    @Transactional(readOnly = true)
    public PagedResponse<BonCommande> getBonsCommandeByPartenaire(Long partenaireId, Pageable pageable) {
        if (!partenaireRepository.existsById(partenaireId)) {
            throw new ResourceNotFoundException("Partenaire", partenaireId);
        }
        return PagedResponse.of(bonCommandeRepository.findByPartenaireId(partenaireId, pageable)
                .map(bonCommandeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public BonCommande getBonCommandeById(Long partenaireId, Long bonId) {
        if (!partenaireRepository.existsById(partenaireId)) {
            throw new ResourceNotFoundException("Partenaire", partenaireId);
        }
        BonCommandeEntity entity = bonCommandeRepository.findById(bonId)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", bonId));
        return bonCommandeMapper.toDto(entity);
    }

    @Transactional
    public BonCommande createBonCommande(Long partenaireId, BonCommandeRequest request) {
        PartenaireEntity partenaire = partenaireRepository.findById(partenaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Partenaire", partenaireId));

        BonCommandeEntity entity = bonCommandeMapper.toEntity(request);
        entity.setPartenaire(partenaire);

        BonCommandeEntity saved = bonCommandeRepository.save(entity);
        return bonCommandeMapper.toDto(saved);
    }

    @Transactional
    public void deleteBonCommande(Long partenaireId, Long bonId) {
        if (!partenaireRepository.existsById(partenaireId)) {
            throw new ResourceNotFoundException("Partenaire", partenaireId);
        }
        if (!bonCommandeRepository.existsById(bonId)) {
            throw new ResourceNotFoundException("Bon de commande", bonId);
        }
        bonCommandeRepository.deleteById(bonId);
    }
}
