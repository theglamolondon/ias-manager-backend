package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Intervention {
	private Long id;
	private LocalDate dhmsDebut;
	private LocalDate dhmsFin;
	private String objet;
	private String details;
	private Long cout;
	private InterventionStatut statut;
	private TypeIntervention typeIntervention;
	private Vehicule vehicule;
	private Partenaire garage;

	/** Non nulle = la dépense a été enregistrée en trésorerie (indépendant de {@link #statut}). */
	private LocalDate dhmsPaiement;
	private Long comptePaiementId;
	private String comptePaiementIntitule;
}
