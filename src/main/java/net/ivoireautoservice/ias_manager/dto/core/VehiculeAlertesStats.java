package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehiculeAlertesStats {
    // Assurances
    private long assurancesExpirees;
    private long assurancesExpirentBientot;
    private List<AlerteVehicule> vehiculesAssuranceExpiree;
    private List<AlerteVehicule> vehiculesAssuranceExpireBientot;

    // Visites techniques
    private long visitesExpirees;
    private long visitesExpirentBientot;
    private List<AlerteVehicule> vehiculesVisiteExpiree;
    private List<AlerteVehicule> vehiculesVisiteExpireBientot;

    // Patentes
    private long patentesExpirees;
    private long patentesExpirentBientot;
    private List<AlerteVehicule> vehiculesPatentExpiree;
    private List<AlerteVehicule> vehiculesPatentExpireBientot;

    // Cartes de stationnement
    private long cartesStationnementExpirees;
    private long cartesStationnementExpirentBientot;
    private List<AlerteVehicule> vehiculesCarteStationnementExpiree;
    private List<AlerteVehicule> vehiculesCarteStationnementExpireBientot;

    // Cartes de transport
    private long cartesTransportExpirees;
    private long cartesTransportExpirentBientot;
    private List<AlerteVehicule> vehiculesCarteTransportExpiree;
    private List<AlerteVehicule> vehiculesCarteTransportExpireBientot;
}
