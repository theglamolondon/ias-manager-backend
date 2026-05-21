package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.core.LigneBonCommande;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneBonCommandeRequest;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.LigneBonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.BonCommandeMapper;
import net.ivoireautoservice.ias_manager.mapper.LigneBonCommandeMapper;
import net.ivoireautoservice.ias_manager.repository.BonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.LigneBonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.ProduitRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BonCommandeService {

    private final BonCommandeRepository bonCommandeRepository;
    private final LigneBonCommandeRepository ligneBonCommandeRepository;
    private final PartenaireRepository partenaireRepository;
    private final ProduitRepository produitRepository;
    private final BonCommandeMapper bonCommandeMapper;
    private final LigneBonCommandeMapper ligneBonCommandeMapper;

    // ==================== BON DE COMMANDE ====================

    @Transactional(readOnly = true)
    public PagedResponse<BonCommande> getAll(String keyword, Long partenaireId, BonCommandeStatusEnum statut, Pageable pageable) {
        String k = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return PagedResponse.of(bonCommandeRepository.search(k, partenaireId, statut, pageable)
                .map(this::toDtoWithItems));
    }

    @Transactional(readOnly = true)
    public BonCommande getById(Long id) {
        BonCommandeEntity entity = bonCommandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", id));
        return toDtoWithItems(entity);
    }

    @Transactional(readOnly = true)
    public BonCommande getByNumero(String numero) {
        BonCommandeEntity entity = bonCommandeRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande avec numéro " + numero + " non trouvé"));
        return toDtoWithItems(entity);
    }

    @Transactional
    public BonCommande create(BonCommandeRequest request) {
        if (request.getPartenaireId() == null) {
            throw new BadRequestException("Le fournisseur (partenaireId) est obligatoire");
        }
        PartenaireEntity partenaire = partenaireRepository.findById(request.getPartenaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getPartenaireId()));
        if (!Boolean.TRUE.equals(partenaire.getIsFournisseur())) {
            throw new BadRequestException("Le partenaire désigné n'est pas un fournisseur");
        }

        BonCommandeEntity entity = bonCommandeMapper.toEntity(request);
        entity.setPartenaire(partenaire);
        entity.setStatut(BonCommandeStatusEnum.CREE);
        entity.setNumero(generateNumero());
        if (entity.getDateCommande() == null) {
            entity.setDateCommande(LocalDate.now());
        }

        BonCommandeEntity saved = bonCommandeRepository.save(entity);

        List<LigneBonCommandeEntity> lignes = saveLignes(saved, request.getItems());

        BonCommande dto = bonCommandeMapper.toDto(saved);
        dto.setItems(ligneBonCommandeMapper.toDtoList(lignes));
        return dto;
    }

    @Transactional
    public BonCommande update(Long id, BonCommandeRequest request) {
        BonCommandeEntity entity = bonCommandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", id));

        if (entity.getStatut() != BonCommandeStatusEnum.CREE) {
            throw new BadRequestException("Seul un bon de commande au statut CREE peut être modifié");
        }

        bonCommandeMapper.updateEntity(request, entity);

        if (request.getPartenaireId() != null) {
            PartenaireEntity partenaire = partenaireRepository.findById(request.getPartenaireId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getPartenaireId()));
            if (!Boolean.TRUE.equals(partenaire.getIsFournisseur())) {
                throw new BadRequestException("Le partenaire désigné n'est pas un fournisseur");
            }
            entity.setPartenaire(partenaire);
        }

        BonCommandeEntity saved = bonCommandeRepository.save(entity);
        return toDtoWithItems(saved);
    }

    @Transactional
    public void delete(Long id) {
        BonCommandeEntity entity = bonCommandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", id));
        if (entity.getStatut() == BonCommandeStatusEnum.PARTIELLEMENT_LIVRE
                || entity.getStatut() == BonCommandeStatusEnum.LIVRE) {
            throw new BadRequestException("Impossible de supprimer un bon de commande ayant fait l'objet d'une livraison");
        }
        ligneBonCommandeRepository.deleteByBonCommandeId(id);
        bonCommandeRepository.delete(entity);
    }

    // ==================== TRANSITIONS DE STATUT ====================

    @Transactional
    public BonCommande valider(Long id) {
        BonCommandeEntity entity = bonCommandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", id));

        if (entity.getStatut() != BonCommandeStatusEnum.CREE) {
            throw new BadRequestException("Seul un bon de commande au statut CREE peut être validé");
        }

        List<LigneBonCommandeEntity> lignes = ligneBonCommandeRepository.findByBonCommandeId(id);
        if (lignes.isEmpty()) {
            throw new BadRequestException("Impossible de valider un bon de commande sans ligne");
        }

        entity.setStatut(BonCommandeStatusEnum.VALIDE);
        BonCommandeEntity saved = bonCommandeRepository.save(entity);
        return toDtoWithItems(saved);
    }

    @Transactional
    public BonCommande annuler(Long id) {
        BonCommandeEntity entity = bonCommandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", id));

        if (entity.getStatut() == BonCommandeStatusEnum.LIVRE) {
            throw new BadRequestException("Impossible d'annuler un bon de commande déjà livré");
        }
        if (entity.getStatut() == BonCommandeStatusEnum.PARTIELLEMENT_LIVRE) {
            throw new BadRequestException("Impossible d'annuler un bon de commande déjà partiellement livré");
        }
        if (entity.getStatut() == BonCommandeStatusEnum.ANNULE) {
            throw new BadRequestException("Le bon de commande est déjà annulé");
        }

        entity.setStatut(BonCommandeStatusEnum.ANNULE);
        BonCommandeEntity saved = bonCommandeRepository.save(entity);
        return toDtoWithItems(saved);
    }

    // ==================== LIGNES BC ====================

    @Transactional(readOnly = true)
    public List<LigneBonCommande> getLignes(Long bonCommandeId) {
        if (!bonCommandeRepository.existsById(bonCommandeId)) {
            throw new ResourceNotFoundException("Bon de commande", bonCommandeId);
        }
        return ligneBonCommandeMapper.toDtoList(
                ligneBonCommandeRepository.findByBonCommandeId(bonCommandeId));
    }

    @Transactional
    public LigneBonCommande createLigne(Long bonCommandeId, LigneBonCommandeRequest request) {
        BonCommandeEntity bc = bonCommandeRepository.findById(bonCommandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", bonCommandeId));

        if (bc.getStatut() != BonCommandeStatusEnum.CREE) {
            throw new BadRequestException("Les lignes ne peuvent être modifiées que lorsque le bon de commande est au statut CREE");
        }

        LigneBonCommandeEntity entity = ligneBonCommandeMapper.toEntity(request);
        entity.setBonCommande(bc);
        entity.setQteLivree(0L);
        resolveProduit(request, entity);

        LigneBonCommandeEntity saved = ligneBonCommandeRepository.save(entity);
        return ligneBonCommandeMapper.toDto(saved);
    }

    @Transactional
    public LigneBonCommande updateLigne(Long bonCommandeId, Long ligneId, LigneBonCommandeRequest request) {
        BonCommandeEntity bc = bonCommandeRepository.findById(bonCommandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", bonCommandeId));

        if (bc.getStatut() != BonCommandeStatusEnum.CREE) {
            throw new BadRequestException("Les lignes ne peuvent être modifiées que lorsque le bon de commande est au statut CREE");
        }

        LigneBonCommandeEntity entity = ligneBonCommandeRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne bon de commande", ligneId));

        if (!entity.getBonCommande().getId().equals(bonCommandeId)) {
            throw new ResourceNotFoundException("Ligne " + ligneId + " n'appartient pas au bon de commande " + bonCommandeId);
        }

        ligneBonCommandeMapper.updateEntity(request, entity);
        resolveProduit(request, entity);

        LigneBonCommandeEntity saved = ligneBonCommandeRepository.save(entity);
        return ligneBonCommandeMapper.toDto(saved);
    }

    @Transactional
    public void deleteLigne(Long bonCommandeId, Long ligneId) {
        BonCommandeEntity bc = bonCommandeRepository.findById(bonCommandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", bonCommandeId));

        if (bc.getStatut() != BonCommandeStatusEnum.CREE) {
            throw new BadRequestException("Les lignes ne peuvent être modifiées que lorsque le bon de commande est au statut CREE");
        }

        LigneBonCommandeEntity entity = ligneBonCommandeRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne bon de commande", ligneId));

        if (!entity.getBonCommande().getId().equals(bonCommandeId)) {
            throw new ResourceNotFoundException("Ligne " + ligneId + " n'appartient pas au bon de commande " + bonCommandeId);
        }

        ligneBonCommandeRepository.delete(entity);
    }

    // ==================== HELPERS ====================

    private String generateNumero() {
        int annee = LocalDate.now().getYear();
        String prefix = "BC-" + annee + "-";
        Integer maxSuffix = bonCommandeRepository.findMaxNumeroSuffix(prefix);
        int next = (maxSuffix != null ? maxSuffix : 0) + 1;
        return String.format("%s%03d", prefix, next);
    }

    private List<LigneBonCommandeEntity> saveLignes(BonCommandeEntity bc, List<LigneBonCommandeRequest> items) {
        List<LigneBonCommandeEntity> lignes = new ArrayList<>();
        if (items == null || items.isEmpty()) return lignes;

        for (LigneBonCommandeRequest item : items) {
            LigneBonCommandeEntity ligne = ligneBonCommandeMapper.toEntity(item);
            ligne.setBonCommande(bc);
            ligne.setQteLivree(0L);
            resolveProduit(item, ligne);
            lignes.add(ligneBonCommandeRepository.save(ligne));
        }
        return lignes;
    }

    private void resolveProduit(LigneBonCommandeRequest request, LigneBonCommandeEntity entity) {
        if (request.getProduitId() != null) {
            ProduitEntity produit = produitRepository.findById(request.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProduitId()));
            entity.setProduit(produit);
        } else {
            entity.setProduit(null);
        }
    }

    private BonCommande toDtoWithItems(BonCommandeEntity entity) {
        BonCommande dto = bonCommandeMapper.toDto(entity);
        List<LigneBonCommandeEntity> lignes = ligneBonCommandeRepository.findByBonCommandeId(entity.getId());
        dto.setItems(ligneBonCommandeMapper.toDtoList(lignes));
        return dto;
    }
}
