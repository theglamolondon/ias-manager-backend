package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehiculeHistorique {
    private Vehicule vehicule;

    // Résumé financier
    private long totalGains;         // revenus missions (factures client payées)
    private long totalDepenses;      // interventions + dépenses missions
    private long totalDepensesMissions;
    private long totalDepensesInterventions;
    private long solde;              // gains - dépenses

    private List<MissionHistorique> missions;
    private List<InterventionHistorique> interventions;
}
