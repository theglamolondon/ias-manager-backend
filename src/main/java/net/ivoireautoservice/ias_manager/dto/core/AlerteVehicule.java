package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlerteVehicule {
    private Long id;
    private String immatriculation;
    private String marque;
    private String photoAvantId;
    private LocalDate finValiditeAssurance;
    private LocalDate finValiditeVisite;
    private LocalDate finValiditePatente;
    private LocalDate finValiditeCarteStationnement;
    private LocalDate finValiditeCarteTransport;
    private long joursRestantsAssurance;
    private long joursRestantsVisite;
    private long joursRestantsPatente;
    private long joursRestantsCarteStationnement;
    private long joursRestantsCarteTransport;
}
