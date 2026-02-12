package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.EntreeProduit;
import net.ivoireautoservice.ias_manager.dto.core.EntreeStock;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Produit;
import net.ivoireautoservice.ias_manager.dto.request.EntreeStockRequest;
import net.ivoireautoservice.ias_manager.dto.request.ProduitRequest;
import net.ivoireautoservice.ias_manager.entity.EntreeProduitEntity;
import net.ivoireautoservice.ias_manager.entity.FamilleProduitEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EntreeProduitMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonFournisseurMapper;
import net.ivoireautoservice.ias_manager.mapper.ProduitMapper;
import net.ivoireautoservice.ias_manager.repository.EntreeProduitRepository;
import net.ivoireautoservice.ias_manager.repository.FamilleProduitRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonFournisseurRepository;
import net.ivoireautoservice.ias_manager.repository.ProduitRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final FamilleProduitRepository familleProduitRepository;
    private final LivraisonFournisseurRepository livraisonFournisseurRepository;
    private final EntreeProduitRepository entreeProduitRepository;
    private final ProduitMapper produitMapper;
    private final EntreeProduitMapper entreeProduitMapper;
    private final LivraisonFournisseurMapper livraisonFournisseurMapper;

    @Transactional(readOnly = true)
    public PagedResponse<Produit> getAllProduits(Pageable pageable) {
        return PagedResponse.of(produitRepository.findAll(pageable).map(produitMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Produit getProduitById(Long id) {
        ProduitEntity entity = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        return produitMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Produit getProduitByReference(String reference) {
        ProduitEntity entity = produitRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Produit avec référence " + reference + " non trouvé"));
        return produitMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<Produit> getProduitsByFamille(Long familleId, Pageable pageable) {
        if (!familleProduitRepository.existsById(familleId)) {
            throw new ResourceNotFoundException("Famille de produit", familleId);
        }
        return PagedResponse.of(produitRepository.findByFamilleId(familleId, pageable).map(produitMapper::toDto));
    }

    @Transactional
    public Produit createProduit(ProduitRequest request) {
        FamilleProduitEntity famille = familleProduitRepository.findById(request.getFamilleId())
                .orElseThrow(() -> new ResourceNotFoundException("Famille de produit", request.getFamilleId()));

        ProduitEntity entity = produitMapper.toEntity(request);
        entity.setFamille(famille);

        ProduitEntity saved = produitRepository.save(entity);
        return produitMapper.toDto(saved);
    }

    @Transactional
    public Produit updateProduit(Long id, ProduitRequest request) {
        ProduitEntity entity = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));

        FamilleProduitEntity famille = familleProduitRepository.findById(request.getFamilleId())
                .orElseThrow(() -> new ResourceNotFoundException("Famille de produit", request.getFamilleId()));

        produitMapper.updateEntity(request, entity);
        entity.setFamille(famille);

        ProduitEntity saved = produitRepository.save(entity);
        return produitMapper.toDto(saved);
    }

    @Transactional
    public void deleteProduit(Long id) {
        if (!produitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produit", id);
        }
        produitRepository.deleteById(id);
    }

    @Transactional
    public EntreeStock enregistrerEntreeStock(EntreeStockRequest request) {
        // 1. Créer la livraison fournisseur
        LivraisonFournisseurEntity livraison = LivraisonFournisseurEntity.builder()
                .numero(request.getNumeroLivraison())
                .dhmsLivraison(request.getDhmsLivraison() != null ? request.getDhmsLivraison() : LocalDateTime.now())
                .build();
        livraison = livraisonFournisseurRepository.save(livraison);

        // 2. Créer les entrées produit et mettre à jour le stock
        var entrees = new ArrayList<EntreeProduitEntity>();
        for (EntreeStockRequest.LigneEntree ligne : request.getLignes()) {
            ProduitEntity produit = produitRepository.findById(ligne.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligne.getProduitId()));

            EntreeProduitEntity entree = EntreeProduitEntity.builder()
                    .quantite(ligne.getQuantite())
                    .produit(produit)
                    .livraisonFournisseur(livraison)
                    .build();
            entrees.add(entreeProduitRepository.save(entree));

            // Mise à jour du stock
            Long stockActuel = produit.getStock() != null ? produit.getStock() : 0L;
            produit.setStock(stockActuel + ligne.getQuantite());
            produitRepository.save(produit);
        }

        // 3. Construire la réponse
        return EntreeStock.builder()
                .livraison(livraisonFournisseurMapper.toDto(livraison))
                .entrees(entreeProduitMapper.toDtoList(entrees))
                .build();
    }
}
