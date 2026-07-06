package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MissionHistorique {
    private Long id;
    private String codeMission;
    private String destination;
    private TypeTarificationEnum typeTarification;
    private LocalDateTime dhmsDebutPrevi;
    private LocalDateTime dhmsFinPrevi;
    private LocalDateTime dhmsDebutReel;
    private LocalDateTime dhmsFinReel;
    private Long dureeLocation;
    private BigDecimal montantTotalHT;
    private String clientNom;
    private String chauffeurNom;

    // Annulation : true si la mission a été annulée (dhmsAnnulation renseigné)
    private boolean annulee;

    // Dépenses mission
    private long totalDepenses;
    private List<DepenseMission> depenses;

    // Lien facture
    private Long factureId;
    private String numFacture;
    private FactureStatusEnum factureStatut;
    private Long montantFactureTtc;
}
