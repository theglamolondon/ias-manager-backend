package net.ivoireautoservice.ias_manager.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportVehiculeResult {
    private int ligne;
    private String immatriculation;
    private String numChassis;
    private boolean success;
    private String message;
}
