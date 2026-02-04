package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.EntreeProduitRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.dto.request.SortieProduitRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EntreeProduitMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonClientMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonFournisseurMapper;
import net.ivoireautoservice.ias_manager.mapper.SortieProduitMapper;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LivraisonService {

    private final LivraisonClientRepository livraisonClientRepository;
    private final SortieProduitRepository sortieProduitRepository;
    private final LivraisonFournisseurRepository livraisonFournisseurRepository;
    private final EntreeProduitRepository entreeProduitRepository;
    private final ProduitRepository produitRepository;
    private final LivraisonClientMapper livraisonClientMapper;
    private final SortieProduitMapper sortieProduitMapper;
    private final LivraisonFournisseurMapper livraisonFournisseurMapper;
    private final EntreeProduitMapper entreeProduitMapper;

    // ==================== LIVRAISONS CLIENT ====================

    @Transactional(readOnly = true)
    public PagedResponse<LivraisonClient> getAllLivraisonsClient(Pageable pageable) {
        return PagedResponse.of(livraisonClientRepository.findAll(pageable).map(livraisonClientMapper::toDto));
    }

    @Transactional(readOnly = true)
    public LivraisonClient getLivraisonClientById(Long id) {
        LivraisonClientEntity entity = livraisonClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison client", id));
        return livraisonClientMapper.toDto(entity);
    }

    @Transactional
    public LivraisonClient createLivraisonClient(LivraisonClientRequest request) {
        LivraisonClientEntity entity = livraisonClientMapper.toEntity(request);
        LivraisonClientEntity saved = livraisonClientRepository.save(entity);
        return livraisonClientMapper.toDto(saved);
    }

    @Transactional
    public LivraisonClient updateLivraisonClient(Long id, LivraisonClientRequest request) {
        LivraisonClientEntity entity = livraisonClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison client", id));
        livraisonClientMapper.updateEntity(request, entity);
        LivraisonClientEntity saved = livraisonClientRepository.save(entity);
        return livraisonClientMapper.toDto(saved);
    }

    @Transactional
    public void deleteLivraisonClient(Long id) {
        if (!livraisonClientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Livraison client", id);
        }
        livraisonClientRepository.deleteById(id);
    }

    // ==================== SORTIES PRODUIT ====================

    @Transactional(readOnly = true)
    public PagedResponse<SortieProduit> getSortiesByLivraison(Long livraisonId, Pageable pageable) {
        if (!livraisonClientRepository.existsById(livraisonId)) {
            throw new ResourceNotFoundException("Livraison client", livraisonId);
        }
        return PagedResponse.of(sortieProduitRepository.findByLivraisonClientId(livraisonId, pageable)
                .map(sortieProduitMapper::toDto));
    }

    @Transactional
    public SortieProduit createSortieProduit(Long livraisonId, SortieProduitRequest request) {
        LivraisonClientEntity livraison = livraisonClientRepository.findById(livraisonId)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison client", livraisonId));

        ProduitEntity produit = produitRepository.findById(request.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProduitId()));

        SortieProduitEntity entity = sortieProduitMapper.toEntity(request);
        entity.setLivraisonClient(livraison);
        entity.setProduit(produit);

        SortieProduitEntity saved = sortieProduitRepository.save(entity);
        return sortieProduitMapper.toDto(saved);
    }

    @Transactional
    public void deleteSortieProduit(Long livraisonId, Long sortieId) {
        if (!livraisonClientRepository.existsById(livraisonId)) {
            throw new ResourceNotFoundException("Livraison client", livraisonId);
        }
        if (!sortieProduitRepository.existsById(sortieId)) {
            throw new ResourceNotFoundException("Sortie produit", sortieId);
        }
        sortieProduitRepository.deleteById(sortieId);
    }

    // ==================== LIVRAISONS FOURNISSEUR ====================

    @Transactional(readOnly = true)
    public PagedResponse<LivraisonFournisseur> getAllLivraisonsFournisseur(Pageable pageable) {
        return PagedResponse.of(livraisonFournisseurRepository.findAll(pageable).map(livraisonFournisseurMapper::toDto));
    }

    @Transactional(readOnly = true)
    public LivraisonFournisseur getLivraisonFournisseurById(Long id) {
        LivraisonFournisseurEntity entity = livraisonFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", id));
        return livraisonFournisseurMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public LivraisonFournisseur getLivraisonFournisseurByNumero(String numero) {
        LivraisonFournisseurEntity entity = livraisonFournisseurRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur avec numéro " + numero + " non trouvé"));
        return livraisonFournisseurMapper.toDto(entity);
    }

    @Transactional
    public LivraisonFournisseur createLivraisonFournisseur(LivraisonFournisseurRequest request) {
        LivraisonFournisseurEntity entity = livraisonFournisseurMapper.toEntity(request);
        LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(entity);
        return livraisonFournisseurMapper.toDto(saved);
    }

    @Transactional
    public LivraisonFournisseur updateLivraisonFournisseur(Long id, LivraisonFournisseurRequest request) {
        LivraisonFournisseurEntity entity = livraisonFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", id));
        livraisonFournisseurMapper.updateEntity(request, entity);
        LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(entity);
        return livraisonFournisseurMapper.toDto(saved);
    }

    @Transactional
    public void deleteLivraisonFournisseur(Long id) {
        if (!livraisonFournisseurRepository.existsById(id)) {
            throw new ResourceNotFoundException("Livraison fournisseur", id);
        }
        livraisonFournisseurRepository.deleteById(id);
    }

    // ==================== ENTREES PRODUIT ====================

    @Transactional(readOnly = true)
    public PagedResponse<EntreeProduit> getEntreesByLivraison(Long livraisonId, Pageable pageable) {
        if (!livraisonFournisseurRepository.existsById(livraisonId)) {
            throw new ResourceNotFoundException("Livraison fournisseur", livraisonId);
        }
        return PagedResponse.of(entreeProduitRepository.findByLivraisonFournisseurId(livraisonId, pageable)
                .map(entreeProduitMapper::toDto));
    }

    @Transactional
    public EntreeProduit createEntreeProduit(Long livraisonId, EntreeProduitRequest request) {
        LivraisonFournisseurEntity livraison = livraisonFournisseurRepository.findById(livraisonId)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", livraisonId));

        ProduitEntity produit = produitRepository.findById(request.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProduitId()));

        EntreeProduitEntity entity = entreeProduitMapper.toEntity(request);
        entity.setLivraisonFournisseur(livraison);
        entity.setProduit(produit);

        EntreeProduitEntity saved = entreeProduitRepository.save(entity);
        return entreeProduitMapper.toDto(saved);
    }

    @Transactional
    public void deleteEntreeProduit(Long livraisonId, Long entreeId) {
        if (!livraisonFournisseurRepository.existsById(livraisonId)) {
            throw new ResourceNotFoundException("Livraison fournisseur", livraisonId);
        }
        if (!entreeProduitRepository.existsById(entreeId)) {
            throw new ResourceNotFoundException("Entrée produit", entreeId);
        }
        entreeProduitRepository.deleteById(entreeId);
    }
}
