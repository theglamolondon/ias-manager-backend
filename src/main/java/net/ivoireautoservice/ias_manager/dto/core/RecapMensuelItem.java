package net.ivoireautoservice.ias_manager.dto.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecapMensuelItem {
    private int annee;
    private int mois;
    private long revenus;
    private long depenses;
    private long marge;
    private double tauxUtilisation;
}
