package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.EntreeProduitRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.dto.request.SortieProduitRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EntreeProduitMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonClientMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonFournisseurMapper;
import net.ivoireautoservice.ias_manager.mapper.SortieProduitMapper;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LivraisonService {

    private final LivraisonClientRepository livraisonClientRepository;
    private final SortieProduitRepository sortieProduitRepository;
    private final LivraisonFournisseurRepository livraisonFournisseurRepository;
    private final EntreeProduitRepository entreeProduitRepository;
    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final ProduitRepository produitRepository;
    private final LivraisonClientMapper livraisonClientMapper;
    private final SortieProduitMapper sortieProduitMapper;
    private final LivraisonFournisseurMapper livraisonFournisseurMapper;
    private final EntreeProduitMapper entreeProduitMapper;
    private final PrintService printService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== LIVRAISONS CLIENT ====================

    @Transactional(readOnly = true)
    public PagedResponse<LivraisonClient> getAllLivraisonsClient(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? livraisonClientRepository.searchByKeyword(keyword.trim(), pageable)
                : livraisonClientRepository.findAll(pageable);
        return PagedResponse.of(page.map(livraisonClientMapper::toDto));
    }

    @Transactional(readOnly = true)
    public LivraisonClient getLivraisonClientById(Long id) {
        LivraisonClientEntity entity = livraisonClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison client", id));
        return livraisonClientMapper.toDto(entity);
    }

    @Transactional
    public void deleteLivraisonClient(Long id) {
        if (!livraisonClientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Livraison client", id);
        }
        livraisonClientRepository.deleteById(id);
    }

    @Transactional
    public LivraisonClient enregistrerLivraisonClient(Long factureId) {
        FactureEntity facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", factureId));

        if (facture.getStatut() != FactureStatusEnum.PROFORMA && facture.getStatut() != FactureStatusEnum.PAYEE) {
            throw new BadRequestException("La livraison client nécessite une facture PROFORMA ou PAYEE");
        }

        if (livraisonClientRepository.findByFactureId(factureId).isPresent()) {
            throw new BadRequestException("Cette facture a déjà fait l'objet d'une livraison client");
        }

        // Créer la livraison client
        LivraisonClientEntity livraison = LivraisonClientEntity.builder()
                .dhmsLivraison(LocalDateTime.now())
                .facture(facture)
                .build();
        LivraisonClientEntity savedLivraison = livraisonClientRepository.save(livraison);

        // Créer les sorties produit pour chaque ligne ayant un produit
        List<SortieProduitEntity> sorties = new ArrayList<>();
        List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(factureId);
        for (LigneFactureEntity ligne : lignes) {
            if (ligne.getProduit() != null && ligne.getQte() != null) {
                SortieProduitEntity sortie = SortieProduitEntity.builder()
                        .quantite(ligne.getQte())
                        .livraisonClient(savedLivraison)
                        .produit(ligne.getProduit())
                        .build();
                sorties.add(sortieProduitRepository.save(sortie));
            }
        }

        // Décrémenter le stock
        decrementerStock(facture);

        // Passer le statut à FACTUREE uniquement si PROFORMA
        if (facture.getStatut() == FactureStatusEnum.PROFORMA) {
            facture.setStatut(FactureStatusEnum.FACTUREE);
            factureRepository.save(facture);
        }

        // Construire la réponse
        LivraisonClient dto = livraisonClientMapper.toDto(savedLivraison);
        dto.setSorties(sortieProduitMapper.toDtoList(sorties));
        return dto;
    }

    private void decrementerStock(FactureEntity facture) {
        List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(facture.getId());
        for (LigneFactureEntity ligne : lignes) {
            if (ligne.getProduit() != null && ligne.getQte() != null) {
                ProduitEntity produit = ligne.getProduit();
                long stockActuel = produit.getStock() != null ? produit.getStock() : 0;
                produit.setStock(stockActuel - ligne.getQte());
                produitRepository.save(produit);
            }
        }
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
    public PagedResponse<LivraisonFournisseur> getAllLivraisonsFournisseur(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? livraisonFournisseurRepository.searchByKeyword(keyword.trim(), pageable)
                : livraisonFournisseurRepository.findAll(pageable);
        return PagedResponse.of(page.map(livraisonFournisseurMapper::toDto));
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

        if (request.getFactureId() != null) {
            FactureEntity facture = factureRepository.findById(request.getFactureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", request.getFactureId()));

            if (facture.getStatut() != FactureStatusEnum.PROFORMA && facture.getStatut() != FactureStatusEnum.PAYEE) {
                throw new BadRequestException("La livraison fournisseur nécessite une facture avec le statut PROFORMA ou PAYEE");
            }

            if (livraisonFournisseurRepository.findByFactureId(request.getFactureId()).isPresent()) {
                throw new BadRequestException("Cette facture a déjà fait l'objet d'une livraison fournisseur");
            }

            entity.setFacture(facture);
            LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(entity);

            // Créer les entrées depuis les lignes de la facture
            List<EntreeProduitEntity> entrees = saveEntreesFromFacture(saved, facture);

            // Passer à FACTUREE uniquement si PROFORMA
            if (facture.getStatut() == FactureStatusEnum.PROFORMA) {
                facture.setStatut(FactureStatusEnum.FACTUREE);
                factureRepository.save(facture);
            }

            LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(saved);
            dto.setEntrees(entreeProduitMapper.toDtoList(entrees));
            return dto;
        }

        // Sinon, utiliser les items fournis (comportement existant)
        LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(entity);
        List<EntreeProduitEntity> entrees = saveEntrees(saved, request.getItems());

        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(saved);
        dto.setEntrees(entreeProduitMapper.toDtoList(entrees));
        return dto;
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

    // ==================== PDF ====================

    @Transactional(readOnly = true)
    public byte[] generateBonLivraisonClientPdf(Long id) {
        LivraisonClientEntity livraison = livraisonClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison client", id));

        FactureEntity facture = livraison.getFacture();
        List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(facture.getId());

        Map<String, Object> data = buildBonLivraisonData(livraison, facture, lignes);
        data.put("numeroBl", "BLC-" + livraison.getId());

        return printService.generatePdf("pdf/BonDeLivraison", data);
    }

    @Transactional(readOnly = true)
    public byte[] generateBonLivraisonFournisseurPdf(Long id) {
        LivraisonFournisseurEntity livraison = livraisonFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", id));

        FactureEntity facture = livraison.getFacture();
        if (facture == null) {
            throw new BadRequestException("Cette livraison fournisseur n'est pas liée à une facture");
        }

        List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(facture.getId());

        Map<String, Object> data = buildBonLivraisonData(livraison, facture, lignes);
        data.put("numeroBl", livraison.getNumero() != null ? livraison.getNumero() : "BLF-" + livraison.getId());

        return printService.generatePdf("pdf/BonDeLivraison", data);
    }

    private Map<String, Object> buildBonLivraisonData(BaseLivraisonEntity livraison, FactureEntity facture, List<LigneFactureEntity> lignes) {
        Map<String, Object> data = new HashMap<>();

        data.put("dateLivraison", livraison.getDhmsLivraison() != null
                ? livraison.getDhmsLivraison().format(DATE_FORMATTER)
                : "");
        data.put("refFacture", facture.getNumFacture() != null ? facture.getNumFacture() : facture.getNumProforma());
        data.put("partenaire", facture.getPartenaire());
        data.put("lignes", lignes);
        data.put("observations", "");
        data.put("logoUrl", "classpath:/static/img/logo-ias.png");

        return data;
    }

    // ==================== HELPERS ====================

    private List<EntreeProduitEntity> saveEntreesFromFacture(LivraisonFournisseurEntity livraison, FactureEntity facture) {
        List<EntreeProduitEntity> entrees = new ArrayList<>();
        List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(facture.getId());
        for (LigneFactureEntity ligne : lignes) {
            if (ligne.getProduit() != null && ligne.getQte() != null) {
                EntreeProduitEntity entree = EntreeProduitEntity.builder()
                        .quantite(ligne.getQte())
                        .livraisonFournisseur(livraison)
                        .produit(ligne.getProduit())
                        .build();
                entrees.add(entreeProduitRepository.save(entree));

                // Incrémenter le stock
                ProduitEntity produit = ligne.getProduit();
                long stockActuel = produit.getStock() != null ? produit.getStock() : 0;
                produit.setStock(stockActuel + ligne.getQte());
                produitRepository.save(produit);
            }
        }
        return entrees;
    }

    private List<EntreeProduitEntity> saveEntrees(LivraisonFournisseurEntity livraison, List<EntreeProduitRequest> items) {
        List<EntreeProduitEntity> entrees = new ArrayList<>();
        if (items == null || items.isEmpty()) return entrees;

        for (EntreeProduitRequest item : items) {
            if (item.getProduitId() == null) {
                throw new BadRequestException("Le produitId est obligatoire pour chaque ligne d'entrée");
            }
            ProduitEntity produit = produitRepository.findById(item.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", item.getProduitId()));

            EntreeProduitEntity entree = entreeProduitMapper.toEntity(item);
            entree.setLivraisonFournisseur(livraison);
            entree.setProduit(produit);
            entrees.add(entreeProduitRepository.save(entree));

            // Incrémenter le stock
            long stockActuel = produit.getStock() != null ? produit.getStock() : 0;
            produit.setStock(stockActuel + item.getQuantite());
            produitRepository.save(produit);
        }
        return entrees;
    }
}
