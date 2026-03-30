package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InterventionHistorique {
    private Long id;
    private String objet;
    private String details;
    private LocalDate dhmsDebut;
    private LocalDate dhmsFin;
    private Long cout;
    private InterventionStatut statut;
    private String typeIntervention;
    private String garageNom;
}
