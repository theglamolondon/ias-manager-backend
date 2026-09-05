package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.EntreeProduitRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurItemRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.dto.request.SortieProduitRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureNatureEnum;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureTypeEnum;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;
import net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum;
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
    private final BonCommandeRepository bonCommandeRepository;
    private final LigneBonCommandeRepository ligneBonCommandeRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final LivraisonClientMapper livraisonClientMapper;
    private final SortieProduitMapper sortieProduitMapper;
    private final LivraisonFournisseurMapper livraisonFournisseurMapper;
    private final EntreeProduitMapper entreeProduitMapper;
    private final PrintService printService;
    private final SecurityService securityService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== LIVRAISONS CLIENT ====================

    @Transactional(readOnly = true)
    public PagedResponse<LivraisonClientSummary> getAllLivraisonsClient(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? livraisonClientRepository.searchByKeyword(keyword.trim(), pageable)
                : livraisonClientRepository.findAll(pageable);
        return PagedResponse.of(page.map(livraisonClientMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public LivraisonClient getLivraisonClientById(Long id) {
        LivraisonClientEntity entity = livraisonClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison client", id));
        LivraisonClient dto = livraisonClientMapper.toDto(entity);
        dto.setSorties(sortieProduitMapper.toDtoList(
                sortieProduitRepository.findByLivraisonClientId(id)));
        return dto;
    }

    @Transactional
    public void deleteLivraisonClient(Long id) {
        if (!livraisonClientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Livraison client", id);
        }
        livraisonClientRepository.deleteById(id);
    }

    @Transactional
    public LivraisonClient enregistrerLivraisonClient(Long factureId, LivraisonClientRequest request) {
        FactureEntity facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", factureId));

        // if (facture.getType() == FactureTypeEnum.MISSION) {
        //     throw new BadRequestException("Une facture de type MISSION ne peut pas faire l'objet d'une livraison client");
        // }

        if (facture.getStatut() != FactureStatusEnum.PROFORMA
                && facture.getStatut() != FactureStatusEnum.FACTUREE
                && facture.getStatut() != FactureStatusEnum.PAYEE) {
            throw new BadRequestException("La livraison client nécessite une facture PROFORMA, FACTUREE ou PAYEE");
        }

        if (livraisonClientRepository.findByFactureId(factureId).isPresent()) {
            throw new BadRequestException("Cette facture a déjà fait l'objet d'une livraison client");
        }

        String refFacture = facture.getNumFacture() != null ? facture.getNumFacture() : facture.getNumProforma();
        String objet = (request != null && request.getObjet() != null && !request.getObjet().isBlank())
                ? request.getObjet()
                : "Livraison " + refFacture;

        // Créer la livraison client
        LivraisonClientEntity livraison = LivraisonClientEntity.builder()
                .objet(objet)
                .dhmsLivraison(LocalDateTime.now())
                .facture(facture)
                .createdBy(securityService.getUtilisateurConnecteOrNull())
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

    @Transactional
    public LivraisonFournisseur enregistrerLivraisonFournisseurFromFacture(Long factureId) {
        FactureEntity facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", factureId));

        if (Boolean.TRUE.equals(facture.getFactureClient())) {
            throw new BadRequestException("Impossible de créer un BL fournisseur depuis une facture client");
        }
        if (facture.getType() == FactureTypeEnum.MISSION) {
            throw new BadRequestException("Une facture de type MISSION ne peut pas faire l'objet d'un bon de livraison fournisseur");
        }
        if (facture.getStatut() != FactureStatusEnum.PROFORMA && facture.getStatut() != FactureStatusEnum.FACTUREE) {
            throw new BadRequestException("Le bon de livraison ne peut être généré que pour une facture PROFORMA ou FACTUREE");
        }
        if (livraisonFournisseurRepository.existsByFactureId(factureId)) {
            throw new BadRequestException("Cette facture a déjà fait l'objet d'un bon de livraison fournisseur");
        }

        String numero = "BLF-" + factureId + "-" + System.currentTimeMillis();

        LivraisonFournisseurEntity livraison = LivraisonFournisseurEntity.builder()
                .numero(numero)
                .dhmsLivraison(LocalDateTime.now())
                .facture(facture)
                .statut(StatutBonLivraisonEnum.CREE)
                .createdBy(securityService.getUtilisateurConnecteOrNull())
                .build();
        LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(livraison);

        List<EntreeProduitEntity> entrees = new ArrayList<>();
        List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(factureId);
        for (LigneFactureEntity ligne : lignes) {
            if (ligne.getProduit() != null && ligne.getQte() != null) {
                EntreeProduitEntity entree = EntreeProduitEntity.builder()
                        .quantite(ligne.getQte())
                        .livraisonFournisseur(saved)
                        .produit(ligne.getProduit())
                        .build();
                entrees.add(entreeProduitRepository.save(entree));
            }
        }

        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(saved);
        dto.setEntrees(entreeProduitMapper.toDtoList(entrees));
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
    public PagedResponse<LivraisonFournisseurSummary> getAllLivraisonsFournisseur(String keyword, Pageable pageable) {
        var page = (keyword != null && !keyword.isBlank())
                ? livraisonFournisseurRepository.searchByKeyword(keyword.trim(), pageable)
                : livraisonFournisseurRepository.findAll(pageable);
        return PagedResponse.of(page.map(livraisonFournisseurMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public List<LivraisonFournisseurSummary> getLivraisonsFournisseurFacturables(Long partenaireId) {
        return livraisonFournisseurRepository.findFacturables(StatutBonLivraisonEnum.VALIDE, partenaireId)
                .stream()
                .map(livraisonFournisseurMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public LivraisonFournisseur getLivraisonFournisseurById(Long id) {
        LivraisonFournisseurEntity entity = livraisonFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", id));
        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(entity);
        dto.setEntrees(entreeProduitMapper.toDtoList(
                entreeProduitRepository.findByLivraisonFournisseurId(id)));
        return dto;
    }

    @Transactional(readOnly = true)
    public LivraisonFournisseur getLivraisonFournisseurByNumero(String numero) {
        LivraisonFournisseurEntity entity = livraisonFournisseurRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur avec numéro " + numero + " non trouvé"));
        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(entity);
        dto.setEntrees(entreeProduitMapper.toDtoList(
                entreeProduitRepository.findByLivraisonFournisseurId(entity.getId())));
        return dto;
    }

    @Transactional
    public LivraisonFournisseur createLivraisonFournisseur(LivraisonFournisseurRequest request) {
        if (request.getBonCommandeId() == null) {
            throw new BadRequestException("Le bon de commande (bonCommandeId) est obligatoire");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Au moins une ligne livrée est requise");
        }

        BonCommandeEntity bc = bonCommandeRepository.findById(request.getBonCommandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", request.getBonCommandeId()));

        if (bc.getStatut() != BonCommandeStatusEnum.VALIDE
                && bc.getStatut() != BonCommandeStatusEnum.PARTIELLEMENT_LIVRE) {
            throw new BadRequestException("Seul un bon de commande au statut VALIDE ou PARTIELLEMENT_LIVRE peut être livré");
        }

        List<LigneBonCommandeEntity> lignesBc = ligneBonCommandeRepository.findByBonCommandeId(bc.getId());
        Map<Long, LigneBonCommandeEntity> lignesById = new HashMap<>();
        for (LigneBonCommandeEntity l : lignesBc) lignesById.put(l.getId(), l);

        // Quantité réservée par les BL en cours (statut CREE), pour ne pas dépasser le reste réel à livrer
        Map<Long, Long> qteReserveeParLigne = sumQuantitesByLigneBcForCreeBls(bc.getId());

        for (LivraisonFournisseurItemRequest item : request.getItems()) {
            LigneBonCommandeEntity ligne = lignesById.get(item.getLigneBonCommandeId());
            if (ligne == null) {
                throw new BadRequestException("La ligne " + item.getLigneBonCommandeId()
                        + " n'appartient pas au bon de commande " + bc.getNumero());
            }
            long qteCommande = ligne.getQte() != null ? ligne.getQte() : 0L;
            long qteDejaLivree = ligne.getQteLivree() != null ? ligne.getQteLivree() : 0L;
            long qteReservee = qteReserveeParLigne.getOrDefault(ligne.getId(), 0L);
            long reste = qteCommande - qteDejaLivree - qteReservee;
            if (item.getQuantite() > reste) {
                throw new BadRequestException("La quantité " + item.getQuantite()
                        + " dépasse le reste à livrer (" + reste + ") pour la ligne " + ligne.getId()
                        + " (réf: " + ligne.getReference() + ")");
            }
        }

        // Création du BL au statut CREE — pas d'effet sur le stock, ni sur qteLivree, ni de facture.
        LivraisonFournisseurEntity livraison = LivraisonFournisseurEntity.builder()
                .numero(request.getNumero() != null ? request.getNumero() : generateLivraisonNumero(bc))
                .objet(request.getObjet() != null && !request.getObjet().isBlank() ? request.getObjet() : null)
                .dhmsLivraison(request.getDhmsLivraison() != null ? request.getDhmsLivraison() : LocalDateTime.now())
                .bonCommande(bc)
                .statut(StatutBonLivraisonEnum.CREE)
                .createdBy(securityService.getUtilisateurConnecteOrNull())
                .build();
        LivraisonFournisseurEntity savedLivraison = livraisonFournisseurRepository.save(livraison);

        List<EntreeProduitEntity> entrees = new ArrayList<>();
        for (LivraisonFournisseurItemRequest item : request.getItems()) {
            LigneBonCommandeEntity ligne = lignesById.get(item.getLigneBonCommandeId());
            EntreeProduitEntity entree = EntreeProduitEntity.builder()
                    .quantite(item.getQuantite())
                    .livraisonFournisseur(savedLivraison)
                    .produit(ligne.getProduit())
                    .ligneBonCommande(ligne)
                    .build();
            entrees.add(entreeProduitRepository.save(entree));
        }

        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(savedLivraison);
        dto.setEntrees(entreeProduitMapper.toDtoList(entrees));
        return dto;
    }

    @Transactional
    public LivraisonFournisseur validerLivraisonFournisseur(Long id, boolean facturerMaintenant) {
        LivraisonFournisseurEntity livraison = livraisonFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", id));

        if (livraison.getStatut() != StatutBonLivraisonEnum.CREE) {
            throw new BadRequestException("Seul un bon de livraison au statut CREE peut être validé");
        }

        long nbPieces = pieceJointeRepository.findByOwnerTypeAndOwnerId(
                PieceJointeOwnerTypeEnum.LIVRAISON_FOURNISSEUR, id).size();
        if (nbPieces == 0) {
            throw new BadRequestException("Une pièce jointe est obligatoire avant validation du bon de livraison");
        }

        BonCommandeEntity bc = livraison.getBonCommande();
        List<LigneBonCommandeEntity> lignesBc = ligneBonCommandeRepository.findByBonCommandeId(bc.getId());
        Map<Long, LigneBonCommandeEntity> lignesById = new HashMap<>();
        for (LigneBonCommandeEntity l : lignesBc) lignesById.put(l.getId(), l);

        List<EntreeProduitEntity> entrees = entreeProduitRepository.findByLivraisonFournisseurId(id);

        // Re-vérifie qu'aucune entrée ne dépasse le reste à livrer (cas de BL concurrents validés entre-temps)
        for (EntreeProduitEntity entree : entrees) {
            LigneBonCommandeEntity ligne = entree.getLigneBonCommande();
            if (ligne == null) continue;
            long qteCommande = ligne.getQte() != null ? ligne.getQte() : 0L;
            long qteDejaLivree = ligne.getQteLivree() != null ? ligne.getQteLivree() : 0L;
            long reste = qteCommande - qteDejaLivree;
            if (entree.getQuantite() > reste) {
                throw new BadRequestException("Validation impossible : la quantité " + entree.getQuantite()
                        + " dépasse le reste à livrer (" + reste + ") pour la ligne "
                        + ligne.getId() + " (réf: " + ligne.getReference() + ")");
            }
        }

        // Application des effets : stock, qteLivree, statut BC
        for (EntreeProduitEntity entree : entrees) {
            LigneBonCommandeEntity ligne = entree.getLigneBonCommande();
            if (ligne != null) {
                long deja = ligne.getQteLivree() != null ? ligne.getQteLivree() : 0L;
                ligne.setQteLivree(deja + entree.getQuantite());
                ligneBonCommandeRepository.save(ligne);
            }
            ProduitEntity produit = entree.getProduit();
            if (produit != null) {
                long stockActuel = produit.getStock() != null ? produit.getStock() : 0L;
                produit.setStock(stockActuel + entree.getQuantite());
                produitRepository.save(produit);
            }
        }

        recalculerStatutBc(bc);

        livraison.setStatut(StatutBonLivraisonEnum.VALIDE);
        livraison.setDateValidation(LocalDateTime.now());

        if (facturerMaintenant) {
            List<LivraisonFournisseurItemRequest> items = new ArrayList<>();
            for (EntreeProduitEntity entree : entrees) {
                if (entree.getLigneBonCommande() == null) continue;
                items.add(LivraisonFournisseurItemRequest.builder()
                        .ligneBonCommandeId(entree.getLigneBonCommande().getId())
                        .quantite(entree.getQuantite())
                        .build());
            }
            FactureEntity facture = genererFactureFournisseur(bc, livraison, items, lignesById);
            livraison.setFacture(facture);
        }

        LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(livraison);

        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(saved);
        dto.setEntrees(entreeProduitMapper.toDtoList(entrees));
        return dto;
    }

    @Transactional
    public LivraisonFournisseur annulerLivraisonFournisseur(Long id, BonCommandeStatusEnum statutBcCible) {
        LivraisonFournisseurEntity livraison = livraisonFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", id));

        if (livraison.getStatut() == StatutBonLivraisonEnum.ANNULE) {
            throw new BadRequestException("Le bon de livraison est déjà annulé");
        }

        // Strict : on refuse l'annulation si le BL a déjà été facturé.
        if (livraison.getFacture() != null) {
            throw new BadRequestException("Impossible d'annuler un bon de livraison déjà rattaché à une facture ("
                    + livraison.getFacture().getNumFacture() + "). Veuillez d'abord traiter la facture.");
        }

        BonCommandeEntity bc = livraison.getBonCommande();
        boolean etaitValide = livraison.getStatut() == StatutBonLivraisonEnum.VALIDE;

        // Si le BL était VALIDÉ, défaire les effets : décrémenter stock, retirer la quantité livrée.
        if (etaitValide) {
            List<EntreeProduitEntity> entrees = entreeProduitRepository.findByLivraisonFournisseurId(id);
            for (EntreeProduitEntity entree : entrees) {
                LigneBonCommandeEntity ligne = entree.getLigneBonCommande();
                if (ligne != null) {
                    long deja = ligne.getQteLivree() != null ? ligne.getQteLivree() : 0L;
                    ligne.setQteLivree(Math.max(deja - entree.getQuantite(), 0L));
                    ligneBonCommandeRepository.save(ligne);
                }
                ProduitEntity produit = entree.getProduit();
                if (produit != null) {
                    long stockActuel = produit.getStock() != null ? produit.getStock() : 0L;
                    produit.setStock(Math.max(stockActuel - entree.getQuantite(), 0L));
                    produitRepository.save(produit);
                }
            }
        }

        livraison.setStatut(StatutBonLivraisonEnum.ANNULE);
        livraison.setDateAnnulation(LocalDateTime.now());
        LivraisonFournisseurEntity saved = livraisonFournisseurRepository.save(livraison);

        // Application du statut BC choisi par l'utilisateur, après validation de cohérence.
        appliquerStatutBcApresAnnulation(bc, statutBcCible);

        LivraisonFournisseur dto = livraisonFournisseurMapper.toDto(saved);
        dto.setEntrees(entreeProduitMapper.toDtoList(
                entreeProduitRepository.findByLivraisonFournisseurId(id)));
        return dto;
    }

    private void recalculerStatutBc(BonCommandeEntity bc) {
        List<LigneBonCommandeEntity> lignes = ligneBonCommandeRepository.findByBonCommandeId(bc.getId());
        boolean toutLivre = lignes.stream().allMatch(l -> {
            long qte = l.getQte() != null ? l.getQte() : 0L;
            long livree = l.getQteLivree() != null ? l.getQteLivree() : 0L;
            return livree >= qte;
        });
        boolean rienLivre = lignes.stream().allMatch(l -> {
            long livree = l.getQteLivree() != null ? l.getQteLivree() : 0L;
            return livree == 0L;
        });
        if (toutLivre) {
            bc.setStatut(BonCommandeStatusEnum.LIVRE);
        } else if (rienLivre) {
            bc.setStatut(BonCommandeStatusEnum.VALIDE);
        } else {
            bc.setStatut(BonCommandeStatusEnum.PARTIELLEMENT_LIVRE);
        }
        bonCommandeRepository.save(bc);
    }

    private void appliquerStatutBcApresAnnulation(BonCommandeEntity bc, BonCommandeStatusEnum statutCible) {
        // Recalcule le statut "naturel" du BC à partir des BL non-ANNULE restants
        List<LigneBonCommandeEntity> lignes = ligneBonCommandeRepository.findByBonCommandeId(bc.getId());
        boolean toutLivre = lignes.stream().allMatch(l -> {
            long qte = l.getQte() != null ? l.getQte() : 0L;
            long livree = l.getQteLivree() != null ? l.getQteLivree() : 0L;
            return livree >= qte;
        });
        boolean rienLivre = lignes.stream().allMatch(l -> {
            long livree = l.getQteLivree() != null ? l.getQteLivree() : 0L;
            return livree == 0L;
        });

        BonCommandeStatusEnum naturel = toutLivre ? BonCommandeStatusEnum.LIVRE
                : (rienLivre ? BonCommandeStatusEnum.VALIDE : BonCommandeStatusEnum.PARTIELLEMENT_LIVRE);

        // L'utilisateur peut choisir entre le statut "naturel" ou ANNULE
        if (statutCible != naturel && statutCible != BonCommandeStatusEnum.ANNULE) {
            throw new BadRequestException("Statut BC cible incohérent. Statuts autorisés : "
                    + naturel + " (recalculé) ou " + BonCommandeStatusEnum.ANNULE);
        }

        bc.setStatut(statutCible);
        bonCommandeRepository.save(bc);
    }

    private Map<Long, Long> sumQuantitesByLigneBcForCreeBls(Long bcId) {
        Map<Long, Long> sums = new HashMap<>();
        List<LivraisonFournisseurEntity> bls = livraisonFournisseurRepository.findByBonCommandeId(bcId);
        for (LivraisonFournisseurEntity bl : bls) {
            if (bl.getStatut() != StatutBonLivraisonEnum.CREE) continue;
            for (EntreeProduitEntity entree : entreeProduitRepository.findByLivraisonFournisseurId(bl.getId())) {
                if (entree.getLigneBonCommande() == null) continue;
                sums.merge(entree.getLigneBonCommande().getId(), entree.getQuantite(), Long::sum);
            }
        }
        return sums;
    }

    private String generateLivraisonNumero(BonCommandeEntity bc) {
        return "BLF-" + bc.getNumero() + "-" + System.currentTimeMillis();
    }

    private static final String NUM_PROFORMA_FOURNISSEUR_PREFIX = "DA/01/79/";

    private String generateNumProformaFournisseur() {
        Integer maxSuffix = factureRepository.findMaxNumProformaSuffix(NUM_PROFORMA_FOURNISSEUR_PREFIX);
        int next = (maxSuffix != null ? maxSuffix : 0) + 1;
        return NUM_PROFORMA_FOURNISSEUR_PREFIX + next;
    }

    private FactureEntity genererFactureFournisseur(BonCommandeEntity bc,
                                                    LivraisonFournisseurEntity livraison,
                                                    List<LivraisonFournisseurItemRequest> items,
                                                    Map<Long, LigneBonCommandeEntity> lignesBcById) {
        // Calcul des montants à partir des quantités effectivement livrées
        long montantHt = 0L;
        Float tva = bc.getTva() != null ? bc.getTva() : 0f;

        FactureEntity facture = FactureEntity.builder()
                .factureClient(false)
                .statut(FactureStatusEnum.FACTUREE)
                .nature(FactureNatureEnum.FACTURE)
                .type(FactureTypeEnum.PRODUIT)
                .tva(tva)
                .partenaire(bc.getPartenaire())
                .objet("Facture fournisseur — BC " + bc.getNumero() + " — Livraison " + livraison.getNumero())
                .numProforma(generateNumProformaFournisseur())
                .numFacture("F-" + livraison.getNumero())
                .createdBy(securityService.getUtilisateurConnecteOrNull())
                .build();

        FactureEntity savedFacture = factureRepository.save(facture);

        // Créer les lignes facture en copiant la ligne BC mais avec quantité livrée
        for (LivraisonFournisseurItemRequest item : items) {
            LigneBonCommandeEntity ligneBc = lignesBcById.get(item.getLigneBonCommandeId());
            long prixUnitaire = ligneBc.getPrixUnitaire() != null ? ligneBc.getPrixUnitaire() : 0L;
            float remise = ligneBc.getRemise() != null ? ligneBc.getRemise() : 0f;
            long ligneHt = Math.round(item.getQuantite() * prixUnitaire * (1 - remise / 100f));
            montantHt += ligneHt;

            LigneFactureEntity ligneFacture = LigneFactureEntity.builder()
                    .reference(ligneBc.getReference())
                    .designation(ligneBc.getDesignation())
                    .qte(item.getQuantite())
                    .prixUnitaire(prixUnitaire)
                    .remise(remise)
                    .montantHt(ligneHt)
                    .extraRef(ligneBc.getExtraRef())
                    .facture(savedFacture)
                    .produit(ligneBc.getProduit())
                    .build();
            ligneFactureRepository.save(ligneFacture);
        }

        savedFacture.setMontantHt(montantHt);
        savedFacture.setMontantTtc(Math.round(montantHt * (1.0 + tva / 100.0)));
        return factureRepository.save(savedFacture);
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

        if (livraison.getStatut() != StatutBonLivraisonEnum.VALIDE) {
            throw new BadRequestException("Seul un bon de livraison VALIDE peut être imprimé");
        }

        FactureEntity facture = livraison.getFacture();
        Map<String, Object> data;

        if (facture != null) {
            List<LigneFactureEntity> lignes = ligneFactureRepository.findByFactureId(facture.getId());
            data = buildBonLivraisonData(livraison, facture, lignes);
        } else {
            // BL validé sans facture (différée ou groupée) : on imprime à partir des entrées et du BC source.
            data = buildBonLivraisonDataSansFacture(livraison);
        }
        data.put("numeroBl", livraison.getNumero() != null ? livraison.getNumero() : "BLF-" + livraison.getId());

        return printService.generatePdf("pdf/BonDeLivraison", data);
    }

    private Map<String, Object> buildBonLivraisonDataSansFacture(LivraisonFournisseurEntity livraison) {
        Map<String, Object> data = new HashMap<>();
        BonCommandeEntity bc = livraison.getBonCommande();
        List<EntreeProduitEntity> entrees = entreeProduitRepository.findByLivraisonFournisseurId(livraison.getId());

        // Adapte les entrées en pseudo-lignes facture pour le template d'impression.
        List<LigneFactureEntity> lignes = new ArrayList<>();
        for (EntreeProduitEntity entree : entrees) {
            LigneBonCommandeEntity ligneBc = entree.getLigneBonCommande();
            if (ligneBc == null) continue;
            long prixUnitaire = ligneBc.getPrixUnitaire() != null ? ligneBc.getPrixUnitaire() : 0L;
            float remise = ligneBc.getRemise() != null ? ligneBc.getRemise() : 0f;
            long montantHt = Math.round(entree.getQuantite() * prixUnitaire * (1 - remise / 100f));
            lignes.add(LigneFactureEntity.builder()
                    .reference(ligneBc.getReference())
                    .designation(ligneBc.getDesignation())
                    .qte(entree.getQuantite())
                    .prixUnitaire(prixUnitaire)
                    .remise(remise)
                    .montantHt(montantHt)
                    .extraRef(ligneBc.getExtraRef())
                    .produit(ligneBc.getProduit())
                    .build());
        }

        data.put("dateLivraison", livraison.getDhmsLivraison() != null
                ? livraison.getDhmsLivraison().format(DATE_FORMATTER) : "");
        data.put("refFacture", "Non facturé (BC " + bc.getNumero() + ")");
        data.put("objet", livraison.getObjet());
        data.put("partenaire", bc.getPartenaire());
        data.put("lignes", lignes);
        data.put("observations", "");
        data.put("logoUrl", "classpath:/static/img/logo-ias.png");
        return data;
    }

    private Map<String, Object> buildBonLivraisonData(BaseLivraisonEntity livraison, FactureEntity facture, List<LigneFactureEntity> lignes) {
        Map<String, Object> data = new HashMap<>();

        data.put("dateLivraison", livraison.getDhmsLivraison() != null
                ? livraison.getDhmsLivraison().format(DATE_FORMATTER)
                : "");
        data.put("refFacture", facture.getNumFacture() != null ? facture.getNumFacture() : facture.getNumProforma());
        data.put("objet", livraison.getObjet());
        data.put("partenaire", facture.getPartenaire());
        data.put("lignes", lignes);
        data.put("observations", "");
        data.put("logoUrl", "classpath:/static/img/logo-ias.png");
        data.put("conditionsPaiement", facture.getConditionsPaiement());
        data.put("statutLivraison", facture.getStatutLivraison());
        data.put("validite", facture.getValidite() != null ? facture.getValidite().format(DATE_FORMATTER) : null);

        return data;
    }

    // ==================== HELPERS ====================

}
