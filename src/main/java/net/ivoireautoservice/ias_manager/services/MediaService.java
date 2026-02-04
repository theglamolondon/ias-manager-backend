package net.ivoireautoservice.ias_manager.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.config.MediaProperties;
import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.exception.MaxMediaExceededException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.MediaMapper;
import net.ivoireautoservice.ias_manager.repository.MediaRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;
    private final MediaProperties mediaProperties;

    private Path uploadPath;

    @PostConstruct
    public void init() throws IOException {
        uploadPath = Paths.get(mediaProperties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }

    @Transactional
    public Media uploadMedia(MultipartFile file) {
        validateFileType(file);

        String id = UUID.randomUUID().toString();
        String extension = getExtension(file.getOriginalFilename());
        String storedFilename = id + "." + extension;

        try {
            Path targetPath = uploadPath.resolve(storedFilename).normalize();
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du stockage du fichier : " + file.getOriginalFilename(), e);
        }

        MediaEntity entity = MediaEntity.builder()
                .id(id)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();

        return mediaMapper.toDto(mediaRepository.save(entity));
    }

    @Transactional
    public void deleteMedia(String mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Média avec l'id " + mediaId + " non trouvé"));

        try {
            Path filePath = uploadPath.resolve(media.getStoredFilename()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la suppression du fichier : " + media.getStoredFilename(), e);
        }

        mediaRepository.delete(media);
    }

    public Resource loadFileAsResource(String mediaId) {
        MediaEntity media = getMediaEntity(mediaId);
        try {
            Path filePath = uploadPath.resolve(media.getStoredFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("Fichier non trouvé pour le média " + mediaId);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Fichier non trouvé pour le média " + mediaId);
        }
    }

    public MediaEntity getMediaEntity(String mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Média avec l'id " + mediaId + " non trouvé"));
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new MaxMediaExceededException(
                    "Type de fichier non autorisé : " + contentType + ". Types autorisés : " + ALLOWED_TYPES
            );
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
