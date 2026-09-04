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
    //
    // Le coût du véhicule est un coût ENGAGÉ : chaque dépense est comptée une fois,
    // par son porteur — l'intervention porte son coût dès la clôture, la ligne de
    // trésorerie manuelle porte le sien. Les mouvements générés (règlement d'une
    // intervention, facture) ne sont donc jamais additionnés à ces totaux, sans quoi
    // une réparation réglée pèserait deux fois.
    private long totalGains;                  // revenus missions (factures client payées)
    private long totalDepenses;               // engagé = interventions + dépenses directes
    private long totalDepensesDirectes;       // dépenses de trésorerie imputées au véhicule
    private long totalDepensesInterventions;  // coût des interventions, réglées ou non
    private long totalDepensesDecaissees;     // sorties de caisse réelles, toutes origines
    private long resteAPayer;                 // engagé - décaissé
    private long solde;                       // gains - engagé

    private List<MissionHistorique> missions;
    private List<InterventionHistorique> interventions;

    /** Dépenses imputées au véhicule hors mission (assurance, gardiennage, amende...). */
    private List<DepenseImputee> depensesDirectes;
}
