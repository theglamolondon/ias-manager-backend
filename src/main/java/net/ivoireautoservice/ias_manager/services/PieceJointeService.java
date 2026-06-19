package net.ivoireautoservice.ias_manager.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.config.MediaProperties;
import net.ivoireautoservice.ias_manager.dto.core.PieceJointe;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonClientEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.PieceJointeEntity;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.PieceJointeMapper;
import net.ivoireautoservice.ias_manager.repository.BonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.FactureRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonClientRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonFournisseurRepository;
import net.ivoireautoservice.ias_manager.repository.MediaRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.PieceJointeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PieceJointeService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf"
    );

    private final PieceJointeRepository pieceJointeRepository;
    private final MediaRepository mediaRepository;
    private final PieceJointeMapper pieceJointeMapper;
    private final MediaProperties mediaProperties;
    private final BonCommandeRepository bonCommandeRepository;
    private final LivraisonFournisseurRepository livraisonFournisseurRepository;
    private final LivraisonClientRepository livraisonClientRepository;
    private final FactureRepository factureRepository;
    private final PartenaireRepository partenaireRepository;

    private Path uploadPath;

    @PostConstruct
    public void init() throws IOException {
        uploadPath = Paths.get(mediaProperties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }

    @Transactional(readOnly = true)
    public List<PieceJointe> getByOwner(PieceJointeOwnerTypeEnum ownerType, Long ownerId) {
        verifierOwnerExiste(ownerType, ownerId);
        verifierPermission(ownerType, ownerId, false);
        return pieceJointeMapper.toDtoList(
                pieceJointeRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId));
    }

    @Transactional
    public PieceJointe upload(PieceJointeOwnerTypeEnum ownerType, Long ownerId, MultipartFile file) {
        validerFichier(file);
        verifierOwnerExiste(ownerType, ownerId);
        verifierPermission(ownerType, ownerId, true);

        String id = UUID.randomUUID().toString();
        String extension = getExtension(file.getOriginalFilename());
        String storedFilename = id + "." + extension;

        try {
            Path targetPath = uploadPath.resolve(storedFilename).normalize();
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du stockage du fichier : " + file.getOriginalFilename(), e);
        }

        MediaEntity media = MediaEntity.builder()
                .id(id)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
        MediaEntity savedMedia = mediaRepository.save(media);

        PieceJointeEntity pj = PieceJointeEntity.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .media(savedMedia)
                .build();
        return pieceJointeMapper.toDto(pieceJointeRepository.save(pj));
    }

    @Transactional
    public void delete(Long id) {
        PieceJointeEntity pj = pieceJointeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe", id));

        verifierPermission(pj.getOwnerType(), pj.getOwnerId(), true);

        MediaEntity media = pj.getMedia();
        pieceJointeRepository.delete(pj);

        if (media != null) {
            try {
                Path filePath = uploadPath.resolve(media.getStoredFilename()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Erreur lors de la suppression du fichier : " + media.getStoredFilename(), e);
            }
            mediaRepository.delete(media);
        }
    }

    private void validerFichier(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Type de fichier non autorisé : " + contentType + ". Types autorisés : PNG, JPG, JPEG, PDF");
        }
    }

    private void verifierOwnerExiste(PieceJointeOwnerTypeEnum ownerType, Long ownerId) {
        switch (ownerType) {
            case BON_COMMANDE -> bonCommandeRepository.findById(ownerId)
                    .map(BonCommandeEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bon de commande", ownerId));
            case LIVRAISON_FOURNISSEUR -> livraisonFournisseurRepository.findById(ownerId)
                    .map(LivraisonFournisseurEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Livraison fournisseur", ownerId));
            case LIVRAISON_CLIENT -> livraisonClientRepository.findById(ownerId)
                    .map(LivraisonClientEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Livraison client", ownerId));
            case FACTURE -> factureRepository.findById(ownerId)
                    .map(FactureEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", ownerId));
            case PARTENAIRE -> partenaireRepository.findById(ownerId)
                    .map(PartenaireEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partenaire", ownerId));
        }
    }

    /**
     * Contrôle d'accès (S8) : une pièce jointe hérite des droits de sa ressource
     * propriétaire. Lire les pièces d'une facture client exige {@code FACTURE_CLIENT_READ},
     * en ajouter/supprimer exige {@code FACTURE_CLIENT_CREATE}, etc. Empêche tout
     * utilisateur authentifié d'accéder aux pièces d'une ressource sur laquelle il
     * n'a aucun droit (faille IDOR).
     *
     * @param ecriture {@code true} pour un upload/suppression, {@code false} pour une lecture.
     */
    private void verifierPermission(PieceJointeOwnerTypeEnum ownerType, Long ownerId, boolean ecriture) {
        String permissionRequise = permissionRequise(ownerType, ownerId, ecriture);
        if (!aAutorite(permissionRequise)) {
            throw new AccessDeniedException(
                    "Accès refusé : permission '" + permissionRequise + "' requise");
        }
    }

    private String permissionRequise(PieceJointeOwnerTypeEnum ownerType, Long ownerId, boolean ecriture) {
        return switch (ownerType) {
            case BON_COMMANDE -> ecriture ? "BON_COMMANDE_UPDATE" : "BON_COMMANDE_READ";
            case LIVRAISON_FOURNISSEUR -> ecriture ? "APPRO_UPDATE" : "APPRO_READ";
            case LIVRAISON_CLIENT -> ecriture ? "LIVRAISON_CLIENT_UPDATE" : "LIVRAISON_CLIENT_READ";
            case PARTENAIRE -> ecriture ? "PARTENAIRE_UPDATE" : "PARTENAIRE_READ";
            case FACTURE -> {
                FactureEntity facture = factureRepository.findById(ownerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Facture", ownerId));
                boolean client = Boolean.TRUE.equals(facture.getFactureClient());
                if (client) {
                    yield ecriture ? "FACTURE_CLIENT_CREATE" : "FACTURE_CLIENT_READ";
                }
                yield ecriture ? "FACTURE_FOURNISSEUR_CREATE" : "FACTURE_FOURNISSEUR_READ";
            }
        };
    }

    private boolean aAutorite(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permission::equals);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
