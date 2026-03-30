package net.ivoireautoservice.ias_manager.dto.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecapitulatifMensuel {
    private List<RecapMensuelItem> lignes;
    private long totalRevenus;
    private long totalDepenses;
    private long totalMarge;
    private double tauxUtilisationMoyen;
}
