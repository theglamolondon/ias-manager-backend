package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.config.MediaProperties;
import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.exception.MaxMediaExceededException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.MediaMapper;
import net.ivoireautoservice.ias_manager.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService — stockage et validation des fichiers")
class MediaServiceTest {

	@TempDir
	Path uploadDir;

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private MediaMapper mediaMapper;

	private MediaService service;

	@BeforeEach
	void setUp() throws Exception {
		MediaProperties properties = new MediaProperties();
		properties.setUploadDir(uploadDir.toString());
		service = new MediaService(mediaRepository, mediaMapper, properties);
		service.init();
	}

	private void stubSauvegarde() {
		when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(i -> i.getArgument(0));
		when(mediaMapper.toDto(any(MediaEntity.class))).thenAnswer(i ->
				Media.builder().id(((MediaEntity) i.getArgument(0)).getId()).build());
	}

	@Nested
	@DisplayName("Validation du type de fichier")
	class Validation {

		@ParameterizedTest
		@ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/gif"})
		@DisplayName("uploadMedia accepte les types image autorisés")
		void imagesAutorisees(String contentType) {
			stubSauvegarde();
			var fichier = new MockMultipartFile("f", "photo.png", contentType, new byte[]{1, 2});

			assertThat(service.uploadMedia(fichier)).isNotNull();
		}

		@ParameterizedTest
		@ValueSource(strings = {"application/pdf", "text/plain", "application/zip"})
		@DisplayName("uploadMedia refuse les types non-image")
		void nonImagesRefusees(String contentType) {
			var fichier = new MockMultipartFile("f", "doc.pdf", contentType, new byte[]{1});

			assertThatThrownBy(() -> service.uploadMedia(fichier))
					.isInstanceOf(MaxMediaExceededException.class)
					.hasMessageContaining("Type de fichier non autorisé");
			verify(mediaRepository, never()).save(any());
		}

		@Test
		@DisplayName("un content-type absent est refusé")
		void contentTypeAbsent() {
			var fichier = new MockMultipartFile("f", "x.bin", null, new byte[]{1});

			assertThatThrownBy(() -> service.uploadMedia(fichier))
					.isInstanceOf(MaxMediaExceededException.class);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"application/pdf",
				"application/msword",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"application/vnd.ms-excel",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"image/png"})
		@DisplayName("uploadDocument accepte bureautique et images")
		void documentsAutorises(String contentType) {
			stubSauvegarde();
			var fichier = new MockMultipartFile("f", "doc.pdf", contentType, new byte[]{1});

			assertThat(service.uploadDocument(fichier)).isNotNull();
		}

		@Test
		@DisplayName("uploadDocument refuse un exécutable")
		void documentRefuse() {
			var fichier = new MockMultipartFile("f", "x.exe", "application/x-msdownload", new byte[]{1});

			assertThatThrownBy(() -> service.uploadDocument(fichier))
					.isInstanceOf(MaxMediaExceededException.class);
		}
	}

	@Nested
	@DisplayName("Stockage sur disque")
	class Stockage {

		@Test
		@DisplayName("le fichier est écrit sous un identifiant aléatoire conservant l'extension")
		void nomStocke() {
			stubSauvegarde();
			var fichier = new MockMultipartFile("f", "Photo Avant.PNG", "image/png", new byte[]{1, 2, 3});

			service.uploadMedia(fichier);

			ArgumentCaptor<MediaEntity> captor = ArgumentCaptor.forClass(MediaEntity.class);
			verify(mediaRepository).save(captor.capture());
			MediaEntity entity = captor.getValue();
			assertThat(entity.getStoredFilename()).isEqualTo(entity.getId() + ".png");
			assertThat(entity.getOriginalFilename()).isEqualTo("Photo Avant.PNG");
			assertThat(entity.getContentType()).isEqualTo("image/png");
			assertThat(entity.getSize()).isEqualTo(3L);
			assertThat(uploadDir.resolve(entity.getStoredFilename())).exists();
		}

		@Test
		@DisplayName("un fichier sans extension reçoit l'extension bin")
		void extensionParDefaut() {
			stubSauvegarde();
			var fichier = new MockMultipartFile("f", "sans_extension", "image/png", new byte[]{1});

			service.uploadMedia(fichier);

			ArgumentCaptor<MediaEntity> captor = ArgumentCaptor.forClass(MediaEntity.class);
			verify(mediaRepository).save(captor.capture());
			assertThat(captor.getValue().getStoredFilename()).endsWith(".bin");
		}

		@Test
		@DisplayName("deux téléversements successifs produisent des identifiants distincts")
		void identifiantsDistincts() {
			stubSauvegarde();
			var fichier = new MockMultipartFile("f", "p.png", "image/png", new byte[]{1});

			Media premier = service.uploadMedia(fichier);
			Media second = service.uploadMedia(fichier);

			assertThat(premier.getId()).isNotEqualTo(second.getId());
		}
	}

	@Nested
	@DisplayName("Lecture et suppression")
	class LectureSuppression {

		@Test
		@DisplayName("getMediaEntity lève 404 sur un identifiant inconnu")
		void mediaInconnu() {
			when(mediaRepository.findById("inconnu")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getMediaEntity("inconnu"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("inconnu");
		}

		@Test
		@DisplayName("deleteMedia efface le fichier du disque et la ligne en base")
		void suppression() throws Exception {
			Path fichier = uploadDir.resolve("abc.png");
			Files.write(fichier, new byte[]{1});
			MediaEntity entity = MediaEntity.builder().id("abc").storedFilename("abc.png").build();
			when(mediaRepository.findById("abc")).thenReturn(Optional.of(entity));

			service.deleteMedia("abc");

			assertThat(fichier).doesNotExist();
			verify(mediaRepository).delete(entity);
		}

		@Test
		@DisplayName("deleteMedia sur un média inconnu lève 404")
		void suppressionInconnue() {
			when(mediaRepository.findById("inconnu")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.deleteMedia("inconnu"))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(mediaRepository, never()).delete(any());
		}

		@Test
		@DisplayName("loadFileAsResource retourne le fichier existant")
		void chargementFichier() throws Exception {
			Path fichier = uploadDir.resolve("abc.png");
			Files.write(fichier, new byte[]{1, 2, 3});
			when(mediaRepository.findById("abc"))
					.thenReturn(Optional.of(MediaEntity.builder().id("abc").storedFilename("abc.png").build()));

			var resource = service.loadFileAsResource("abc");

			assertThat(resource.exists()).isTrue();
			assertThat(resource.contentLength()).isEqualTo(3L);
		}

		@Test
		@DisplayName("loadFileAsResource lève 404 quand le fichier a disparu du disque")
		void fichierDisparu() {
			when(mediaRepository.findById("abc"))
					.thenReturn(Optional.of(MediaEntity.builder().id("abc").storedFilename("absent.png").build()));

			assertThatThrownBy(() -> service.loadFileAsResource("abc"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Fichier non trouvé");
		}
	}
}
