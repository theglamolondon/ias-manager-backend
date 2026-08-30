package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Vehicule;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.dto.response.ImportVehiculeResult;
import net.ivoireautoservice.ias_manager.entity.MarqueEntity;
import net.ivoireautoservice.ias_manager.entity.TypeCarburantEntity;
import net.ivoireautoservice.ias_manager.entity.TypeVehiculeEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.repository.MarqueRepository;
import net.ivoireautoservice.ias_manager.repository.TypeCarburantRepository;
import net.ivoireautoservice.ias_manager.repository.TypeVehiculeRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ImportService — import Excel de véhicules et modèle de saisie")
class ImportServiceTest {

	private static final String[] EN_TETES = {
			"Immatriculation*", "N° Châssis*", "Marque*", "Couleur*", "Nb Places*",
			"Type Véhicule*", "Carburant*", "Date Immatriculation", "Date Achat",
			"Coût Achat (FCFA)", "Coût Assurance (FCFA)", "Carte Grise",
			"Type Commercial", "Puissance Fiscale", "Kilométrage",
			"Fin Validité Visite", "Fin Validité Assurance",
			"Fin Validité Patente", "Fin Validité Carte Stationnement", "Fin Validité Carte Transport",
			"Date Mise Circulation", "Concessionnaire", "Date Fin Garantie"
	};

	@Mock private VehiculeRepository vehiculeRepository;
	@Mock private TypeVehiculeRepository typeVehiculeRepository;
	@Mock private TypeCarburantRepository typeCarburantRepository;
	@Mock private MarqueRepository marqueRepository;
	@Mock private VehiculeService vehiculeService;

	@InjectMocks
	private ImportService service;

	@BeforeEach
	void setUp() {
		when(marqueRepository.findAll())
				.thenReturn(List.of(MarqueEntity.builder().id(1L).libelle("Toyota").build()));
		when(typeVehiculeRepository.findAll())
				.thenReturn(List.of(TypeVehiculeEntity.builder().id(2L).libelle("Berline").build()));
		when(typeCarburantRepository.findAll())
				.thenReturn(List.of(TypeCarburantEntity.builder().id(3L).libelle("Diesel").build()));
		when(vehiculeRepository.findByImmatriculation(any())).thenReturn(Optional.empty());
		when(vehiculeRepository.findByNumChassis(any())).thenReturn(Optional.empty());
	}

	/** Construit un classeur d'import minimal : ligne d'en-têtes + lignes de données. */
	private static MockMultipartFile classeur(String[]... lignes) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("Véhicules");
			Row entetes = sheet.createRow(0);
			for (int i = 0; i < EN_TETES.length; i++) {
				entetes.createCell(i).setCellValue(EN_TETES[i]);
			}
			for (int l = 0; l < lignes.length; l++) {
				Row row = sheet.createRow(l + 1);
				for (int c = 0; c < lignes[l].length; c++) {
					row.createCell(c).setCellValue(lignes[l][c]);
				}
			}
			wb.write(out);
			return new MockMultipartFile("file", "import.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					out.toByteArray());
		}
	}

	private static String[] donneesLigne() {
		return new String[]{"AB-123-CD", "VF123", "Toyota", "Blanc", "5", "Berline", "Diesel",
				"2020-01-15", "2020-03-01", "15000000", "500000", "CI-2020-1",
				"Corolla", "5 CV", "25000", "2027-12-31", "2027-06-30",
				"2027-12-31", "2027-12-31", "2027-12-31", "2020-02-01", "CFAO", "2026-12-31"};
	}

	@Nested
	@DisplayName("Import")
	class Import {

		@Test
		@DisplayName("une ligne complète et cohérente est importée")
		void ligneValide() throws Exception {
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(donneesLigne()));

			assertThat(resultats).hasSize(1);
			assertThat(resultats.get(0).isSuccess()).isTrue();
			assertThat(resultats.get(0).getLigne()).isEqualTo(2);
			assertThat(resultats.get(0).getImmatriculation()).isEqualTo("AB-123-CD");
		}

		@Test
		@DisplayName("les libellés de référentiel sont résolus sans tenir compte de la casse")
		void resolutionInsensibleALaCasse() throws Exception {
			String[] ligne = donneesLigne();
			ligne[2] = "TOYOTA";
			ligne[5] = "berline";
			ligne[6] = "DIESEL";
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			service.importVehicules(classeur(ligne));

			ArgumentCaptor<VehiculeRequest> captor = ArgumentCaptor.forClass(VehiculeRequest.class);
			verify(vehiculeService).createVehicule(captor.capture());
			assertThat(captor.getValue().getMarqueId()).isEqualTo(1L);
			assertThat(captor.getValue().getTypeId()).isEqualTo(2L);
			assertThat(captor.getValue().getEnergieId()).isEqualTo(3L);
		}

		@Test
		@DisplayName("les dates et montants sont convertis, les nombres nettoyés")
		void conversions() throws Exception {
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			service.importVehicules(classeur(donneesLigne()));

			ArgumentCaptor<VehiculeRequest> captor = ArgumentCaptor.forClass(VehiculeRequest.class);
			verify(vehiculeService).createVehicule(captor.capture());
			VehiculeRequest request = captor.getValue();
			assertThat(request.getDateAchat()).isEqualTo(LocalDate.of(2020, 3, 1));
			assertThat(request.getCoutAchat()).isEqualTo(15_000_000L);
			assertThat(request.getKilometrage()).isEqualTo(25_000L);
			assertThat(request.getNombrePlaces()).isEqualTo(5);
			assertThat(request.getConcessionnaire()).isEqualTo("CFAO");
		}

		@Test
		@DisplayName("un champ obligatoire manquant produit une erreur ciblée")
		void champObligatoireManquant() throws Exception {
			String[] ligne = donneesLigne();
			ligne[3] = "";

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(ligne));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("Champs obligatoires manquants");
			verify(vehiculeService, never()).createVehicule(any());
		}

		@Test
		@DisplayName("une immatriculation déjà présente est rejetée")
		void immatriculationDupliquee() throws Exception {
			when(vehiculeRepository.findByImmatriculation("AB-123-CD"))
					.thenReturn(Optional.of(new VehiculeEntity()));

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(donneesLigne()));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("Immatriculation déjà existante");
		}

		@Test
		@DisplayName("un numéro de châssis déjà présent est rejeté")
		void chassisDuplique() throws Exception {
			when(vehiculeRepository.findByNumChassis("VF123"))
					.thenReturn(Optional.of(new VehiculeEntity()));

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(donneesLigne()));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("châssis déjà existant");
		}

		@Test
		@DisplayName("une marque inconnue du référentiel est signalée")
		void marqueInconnue() throws Exception {
			String[] ligne = donneesLigne();
			ligne[2] = "Peugeot";

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(ligne));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("Marque introuvable");
		}

		@Test
		@DisplayName("un type de véhicule inconnu est signalé")
		void typeInconnu() throws Exception {
			String[] ligne = donneesLigne();
			ligne[5] = "Camion";

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(ligne));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("Type de véhicule introuvable");
		}

		@Test
		@DisplayName("un carburant inconnu est signalé")
		void carburantInconnu() throws Exception {
			String[] ligne = donneesLigne();
			ligne[6] = "Hydrogène";

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(ligne));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("Carburant introuvable");
		}

		@Test
		@DisplayName("un carburant vide est accepté (champ optionnel côté données)")
		void carburantVide() throws Exception {
			String[] ligne = donneesLigne();
			ligne[6] = "";
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(ligne));

			assertThat(resultats.get(0).isSuccess()).isTrue();
			ArgumentCaptor<VehiculeRequest> captor = ArgumentCaptor.forClass(VehiculeRequest.class);
			verify(vehiculeService).createVehicule(captor.capture());
			assertThat(captor.getValue().getEnergieId()).isNull();
		}

		@Test
		@DisplayName("un nombre de places absent retombe sur 5")
		void nbPlacesParDefaut() throws Exception {
			String[] ligne = donneesLigne();
			ligne[4] = "";
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			service.importVehicules(classeur(ligne));

			ArgumentCaptor<VehiculeRequest> captor = ArgumentCaptor.forClass(VehiculeRequest.class);
			verify(vehiculeService).createVehicule(captor.capture());
			assertThat(captor.getValue().getNombrePlaces()).isEqualTo(5);
		}

		@Test
		@DisplayName("une date illisible est ignorée sans faire échouer la ligne")
		void dateIllisible() throws Exception {
			String[] ligne = donneesLigne();
			ligne[8] = "pas une date";
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(ligne));

			assertThat(resultats.get(0).isSuccess()).isTrue();
			ArgumentCaptor<VehiculeRequest> captor = ArgumentCaptor.forClass(VehiculeRequest.class);
			verify(vehiculeService).createVehicule(captor.capture());
			assertThat(captor.getValue().getDateAchat()).isNull();
		}

		@Test
		@DisplayName("l'échec d'une ligne n'empêche pas le traitement des suivantes")
		void erreurIsolee() throws Exception {
			String[] invalide = donneesLigne();
			invalide[0] = "";
			String[] valide = donneesLigne();
			valide[0] = "EF-456-GH";
			valide[1] = "VF456";
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenReturn(Vehicule.builder().build());

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(invalide, valide));

			assertThat(resultats).hasSize(2);
			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(1).isSuccess()).isTrue();
		}

		@Test
		@DisplayName("une exception de création est rapportée comme échec de la ligne")
		void echecDeCreation() throws Exception {
			when(vehiculeService.createVehicule(any(VehiculeRequest.class)))
					.thenThrow(new RuntimeException("Contrainte violée"));

			List<ImportVehiculeResult> resultats = service.importVehicules(classeur(donneesLigne()));

			assertThat(resultats.get(0).isSuccess()).isFalse();
			assertThat(resultats.get(0).getMessage()).contains("Contrainte violée");
		}

		@Test
		@DisplayName("un fichier illisible remonte une erreur explicite")
		void fichierIllisible() {
			var fichier = new MockMultipartFile("file", "corrompu.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					"ceci n'est pas un classeur".getBytes());

			assertThatThrownBy(() -> service.importVehicules(fichier))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("lecture du fichier Excel");
		}

		@Test
		@DisplayName("un classeur sans données ne produit aucun résultat")
		void classeurVide() throws Exception {
			assertThat(service.importVehicules(classeur())).isEmpty();
		}
	}

	@Nested
	@DisplayName("Modèle de saisie")
	class Modele {

		@Test
		@DisplayName("le modèle contient la feuille de saisie, ses en-têtes et un exemple")
		void structure() throws Exception {
			byte[] contenu = service.generateImportTemplate();

			try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(contenu))) {
				Sheet principale = wb.getSheet("Véhicules");
				assertThat(principale).isNotNull();
				assertThat(principale.getRow(0).getPhysicalNumberOfCells()).isEqualTo(EN_TETES.length);
				assertThat(principale.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Immatriculation*");
				assertThat(principale.getRow(1).getCell(0).getStringCellValue()).isEqualTo("AB-123-CD");
			}
		}

		@Test
		@DisplayName("la feuille Référentiel est présente, masquée et alimentée par la base")
		void referentiel() throws Exception {
			byte[] contenu = service.generateImportTemplate();

			try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(contenu))) {
				int index = wb.getSheetIndex("Référentiel");
				assertThat(index).isNotNegative();
				assertThat(wb.isSheetHidden(index)).isTrue();
				Sheet ref = wb.getSheetAt(index);
				assertThat(ref.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Berline");
				assertThat(ref.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Diesel");
				assertThat(ref.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Toyota");
			}
		}

		@Test
		@DisplayName("des listes déroulantes contraignent marque, type et carburant")
		void listesDeroulantes() throws Exception {
			byte[] contenu = service.generateImportTemplate();

			try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(contenu))) {
				assertThat(wb.getSheet("Véhicules").getDataValidations()).hasSize(3);
			}
		}

		@Test
		@DisplayName("un référentiel vide n'empêche pas la génération du modèle")
		void referentielVide() throws Exception {
			when(marqueRepository.findAll()).thenReturn(List.of());
			when(typeVehiculeRepository.findAll()).thenReturn(List.of());
			when(typeCarburantRepository.findAll()).thenReturn(List.of());

			byte[] contenu = service.generateImportTemplate();

			try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(contenu))) {
				assertThat(wb.getSheet("Véhicules")).isNotNull();
				assertThat(wb.getSheet("Véhicules").getDataValidations()).isEmpty();
			}
		}
	}
}
