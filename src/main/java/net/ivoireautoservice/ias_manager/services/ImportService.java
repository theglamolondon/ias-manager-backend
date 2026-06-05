package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.dto.response.ImportVehiculeResult;
import net.ivoireautoservice.ias_manager.entity.MarqueEntity;
import net.ivoireautoservice.ias_manager.entity.TypeCarburantEntity;
import net.ivoireautoservice.ias_manager.entity.TypeVehiculeEntity;
import net.ivoireautoservice.ias_manager.repository.MarqueRepository;
import net.ivoireautoservice.ias_manager.repository.TypeCarburantRepository;
import net.ivoireautoservice.ias_manager.repository.TypeVehiculeRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final VehiculeRepository vehiculeRepository;
    private final TypeVehiculeRepository typeVehiculeRepository;
    private final TypeCarburantRepository typeCarburantRepository;
    private final MarqueRepository marqueRepository;
    private final VehiculeService vehiculeService;

    private static final String[] HEADERS = {
        "Immatriculation*", "N° Châssis*", "Marque*", "Couleur*", "Nb Places*",
        "Type Véhicule*", "Carburant*", "Date Immatriculation", "Date Achat",
        "Coût Achat (FCFA)", "Coût Assurance (FCFA)", "Carte Grise",
        "Type Commercial", "Puissance Fiscale", "Kilométrage",
        "Fin Validité Visite", "Fin Validité Assurance",
        "Fin Validité Patente", "Fin Validité Carte Stationnement", "Fin Validité Carte Transport",
        "Date Mise Circulation", "Concessionnaire", "Date Fin Garantie"
    };

    private static final int COL_MARQUE       = 2;
    private static final int COL_TYPE_VEHICULE = 5;
    private static final int COL_CARBURANT     = 6;

    // ==================== IMPORT ====================

    @Transactional
    public List<ImportVehiculeResult> importVehicules(MultipartFile file) {
        List<ImportVehiculeResult> results = new ArrayList<>();

        Map<String, Long> marqueMap = new HashMap<>();
        marqueRepository.findAll()
                .forEach(m -> marqueMap.put(m.getLibelle().toLowerCase().trim(), m.getId()));

        Map<String, Long> typeMap = new HashMap<>();
        typeVehiculeRepository.findAll()
                .forEach(t -> typeMap.put(t.getLibelle().toLowerCase().trim(), t.getId()));

        Map<String, Long> energieMap = new HashMap<>();
        typeCarburantRepository.findAll()
                .forEach(e -> energieMap.put(e.getLibelle().toLowerCase().trim(), e.getId()));

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return results;

            Map<String, Integer> colIndex = buildColumnIndex(headerRow);
            DataFormatter formatter = new DataFormatter();

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                processRow(formatter, row, colIndex, rowNum + 1, marqueMap, typeMap, energieMap, results);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier Excel : " + e.getMessage(), e);
        }
        return results;
    }

    private void processRow(DataFormatter formatter, Row row, Map<String, Integer> colIndex,
                            int ligne, Map<String, Long> marqueMap, Map<String, Long> typeMap,
                            Map<String, Long> energieMap, List<ImportVehiculeResult> results) {

        String immatriculation = cell(formatter, row, colIndex, "Immatriculation*");
        String numChassis      = cell(formatter, row, colIndex, "N° Châssis*");
        String marqueLbl       = cell(formatter, row, colIndex, "Marque*");
        String couleur         = cell(formatter, row, colIndex, "Couleur*");
        String typeLbl         = cell(formatter, row, colIndex, "Type Véhicule*");
        String carburantLbl    = cell(formatter, row, colIndex, "Carburant*");

        // Required field check
        if (immatriculation.isEmpty() || numChassis.isEmpty() || marqueLbl.isEmpty() || couleur.isEmpty()) {
            results.add(err(ligne, immatriculation, numChassis,
                    "Champs obligatoires manquants (immatriculation, châssis, marque, couleur)"));
            return;
        }

        // Duplicate check
        if (vehiculeRepository.findByImmatriculation(immatriculation).isPresent()) {
            results.add(err(ligne, immatriculation, numChassis,
                    "Immatriculation déjà existante : " + immatriculation));
            return;
        }
        if (vehiculeRepository.findByNumChassis(numChassis).isPresent()) {
            results.add(err(ligne, immatriculation, numChassis,
                    "Numéro de châssis déjà existant : " + numChassis));
            return;
        }

        // FK resolution by label (case-insensitive)
        Long marqueId = marqueMap.get(marqueLbl.toLowerCase().trim());
        if (marqueId == null) {
            results.add(err(ligne, immatriculation, numChassis,
                    "Marque introuvable : \"" + marqueLbl + "\""));
            return;
        }
        Long typeId = typeMap.get(typeLbl.toLowerCase().trim());
        if (typeId == null) {
            results.add(err(ligne, immatriculation, numChassis,
                    "Type de véhicule introuvable : \"" + typeLbl + "\""));
            return;
        }
        Long energieId = carburantLbl.isEmpty() ? null : energieMap.get(carburantLbl.toLowerCase().trim());
        if (!carburantLbl.isEmpty() && energieId == null) {
            results.add(err(ligne, immatriculation, numChassis,
                    "Carburant introuvable : \"" + carburantLbl + "\""));
            return;
        }

        try {
            String nbStr = cell(formatter, row, colIndex, "Nb Places*");
            int nombrePlaces = nbStr.isEmpty() ? 5 : Integer.parseInt(nbStr.replaceAll("[^0-9]", ""));

            VehiculeRequest req = VehiculeRequest.builder()
                    .immatriculation(immatriculation)
                    .numChassis(numChassis)
                    .marqueId(marqueId)
                    .couleur(couleur)
                    .nombrePlaces(nombrePlaces)
                    .typeId(typeId)
                    .energieId(energieId)
                    .dateImmatriculation(parseDate(cell(formatter, row, colIndex, "Date Immatriculation")))
                    .dateAchat(parseDate(cell(formatter, row, colIndex, "Date Achat")))
                    .coutAchat(parseLong(cell(formatter, row, colIndex, "Coût Achat (FCFA)")))
                    .coutAssurance(parseLong(cell(formatter, row, colIndex, "Coût Assurance (FCFA)")))
                    .carteGrise(cell(formatter, row, colIndex, "Carte Grise"))
                    .typeCommercial(cell(formatter, row, colIndex, "Type Commercial"))
                    .puissanceFiscale(cell(formatter, row, colIndex, "Puissance Fiscale"))
                    .kilometrage(parseLong(cell(formatter, row, colIndex, "Kilométrage")))
                    .finValiditeVisite(parseDate(cell(formatter, row, colIndex, "Fin Validité Visite")))
                    .finValiditeAssurance(parseDate(cell(formatter, row, colIndex, "Fin Validité Assurance")))
                    .finValiditePatente(parseDate(cell(formatter, row, colIndex, "Fin Validité Patente")))
                    .finValiditeCarteStationnement(parseDate(cell(formatter, row, colIndex, "Fin Validité Carte Stationnement")))
                    .finValiditeCarteTransport(parseDate(cell(formatter, row, colIndex, "Fin Validité Carte Transport")))
                    .dateMiseCirculation(parseDate(cell(formatter, row, colIndex, "Date Mise Circulation")))
                    .concessionnaire(cell(formatter, row, colIndex, "Concessionnaire"))
                    .dateFinGarantie(parseDate(cell(formatter, row, colIndex, "Date Fin Garantie")))
                    .build();

            vehiculeService.createVehicule(req);
            results.add(ImportVehiculeResult.builder()
                    .ligne(ligne).immatriculation(immatriculation).numChassis(numChassis)
                    .success(true).message("Importé avec succès").build());

        } catch (Exception e) {
            results.add(err(ligne, immatriculation, numChassis, e.getMessage()));
        }
    }

    // ==================== TEMPLATE ====================

    public byte[] generateImportTemplate() {
        List<MarqueEntity> marques       = marqueRepository.findAll();
        List<TypeVehiculeEntity> types   = typeVehiculeRepository.findAll();
        List<TypeCarburantEntity> energies = typeCarburantRepository.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── Feuille principale ──────────────────────────────────────────
            XSSFSheet main = wb.createSheet("Véhicules");
            main.createFreezePane(0, 1);

            XSSFCellStyle requiredStyle = buildHeaderStyle(wb, new byte[]{(byte) 30, (byte) 64, (byte) 175});   // blue-800
            XSSFCellStyle optionalStyle = buildHeaderStyle(wb, new byte[]{(byte) 59, (byte) 130, (byte) 246});  // blue-400

            Row header = main.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(HEADERS[i].endsWith("*") ? requiredStyle : optionalStyle);
                main.setColumnWidth(i, 26 * 256);
            }

            // Sample row using first real DB values
            Row sample = main.createRow(1);
            sample.createCell(0).setCellValue("AB-123-CD");
            sample.createCell(1).setCellValue("VF12345678901234");
            if (!marques.isEmpty())  sample.createCell(COL_MARQUE).setCellValue(marques.get(0).getLibelle());
            sample.createCell(3).setCellValue("Blanc");
            sample.createCell(4).setCellValue(5);
            if (!types.isEmpty())    sample.createCell(COL_TYPE_VEHICULE).setCellValue(types.get(0).getLibelle());
            if (!energies.isEmpty()) sample.createCell(COL_CARBURANT).setCellValue(energies.get(0).getLibelle());
            sample.createCell(7).setCellValue("2020-01-15");
            sample.createCell(8).setCellValue("2020-03-01");
            sample.createCell(9).setCellValue(15000000);
            sample.createCell(10).setCellValue(500000);
            sample.createCell(11).setCellValue("CI-2020-12345");
            sample.createCell(12).setCellValue("Corolla");
            sample.createCell(13).setCellValue("5 CV");
            sample.createCell(14).setCellValue(25000);
            sample.createCell(15).setCellValue("2025-12-31");
            sample.createCell(16).setCellValue("2025-06-30");
            sample.createCell(17).setCellValue("2025-12-31");
            sample.createCell(18).setCellValue("2025-12-31");
            sample.createCell(19).setCellValue("2025-12-31");
            sample.createCell(20).setCellValue("2020-02-01");
            sample.createCell(21).setCellValue("CFAO Motors");
            sample.createCell(22).setCellValue("2023-12-31");

            // ── Feuille Référentiel (masquée) avec valeurs valides ──────────
            XSSFSheet ref = wb.createSheet("Référentiel");
            wb.setSheetHidden(wb.getSheetIndex("Référentiel"), true);

            Row refHeader = ref.createRow(0);
            refHeader.createCell(0).setCellValue("Type Véhicule");
            refHeader.createCell(1).setCellValue("Carburant");
            refHeader.createCell(2).setCellValue("Marque");

            for (int i = 0; i < types.size(); i++) {
                getOrCreate(ref, i + 1).createCell(0).setCellValue(types.get(i).getLibelle());
            }
            for (int i = 0; i < energies.size(); i++) {
                getOrCreate(ref, i + 1).createCell(1).setCellValue(energies.get(i).getLibelle());
            }
            for (int i = 0; i < marques.size(); i++) {
                getOrCreate(ref, i + 1).createCell(2).setCellValue(marques.get(i).getLibelle());
            }

            // ── Listes déroulantes référencant la feuille Référentiel ───────
            DataValidationHelper dvh = main.getDataValidationHelper();

            if (!marques.isEmpty()) {
                addDropdown(main, dvh,
                        "Référentiel!$C$2:$C$" + (marques.size() + 1),
                        COL_MARQUE,
                        "Marque invalide",
                        "Sélectionnez une marque dans la liste déroulante.");
            }
            if (!types.isEmpty()) {
                addDropdown(main, dvh,
                        "Référentiel!$A$2:$A$" + (types.size() + 1),
                        COL_TYPE_VEHICULE,
                        "Type de véhicule invalide",
                        "Sélectionnez un type dans la liste déroulante.");
            }
            if (!energies.isEmpty()) {
                addDropdown(main, dvh,
                        "Référentiel!$B$2:$B$" + (energies.size() + 1),
                        COL_CARBURANT,
                        "Carburant invalide",
                        "Sélectionnez un carburant dans la liste déroulante.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du modèle Excel", e);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void addDropdown(XSSFSheet sheet, DataValidationHelper dvh,
                             String formula, int col, String errorTitle, String errorMsg) {
        DataValidationConstraint constraint = dvh.createFormulaListConstraint(formula);
        CellRangeAddressList range = new CellRangeAddressList(1, 1000, col, col);
        DataValidation validation = dvh.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox(errorTitle, errorMsg);
        sheet.addValidationData(validation);
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private Row getOrCreate(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        return row != null ? row : sheet.createRow(rowNum);
    }

    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i <= headerRow.getLastCellNum(); i++) {
            Cell c = headerRow.getCell(i);
            if (c != null) index.put(c.getStringCellValue().trim(), i);
        }
        return index;
    }

    private String cell(DataFormatter fmt, Row row, Map<String, Integer> colIndex, String header) {
        Integer idx = colIndex.get(header);
        if (idx == null) return "";
        Cell c = row.getCell(idx);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value.substring(0, 10)); } catch (Exception e) { return null; }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value.replaceAll("[^0-9]", "")); } catch (Exception e) { return null; }
    }

    private ImportVehiculeResult err(int ligne, String immatriculation, String numChassis, String message) {
        return ImportVehiculeResult.builder()
                .ligne(ligne).immatriculation(immatriculation).numChassis(numChassis)
                .success(false).message(message).build();
    }
}
