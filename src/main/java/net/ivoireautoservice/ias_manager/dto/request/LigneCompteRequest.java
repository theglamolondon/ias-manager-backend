package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LigneCompteRequest {

	@NotNull(message = "Le type de mouvement est obligatoire")
	private CompteLigneType type;

	private String objet;

	@NotNull(message = "Le montant est obligatoire")
	@Positive(message = "Le montant doit être positif")
	private Long montant;

	private String observation;

	// ==================== IMPUTATION ====================
	// Réservée aux DEPENSE. Les règles sont conditionnelles (nature obligatoire,
	// exclusivité des trois branches d'imputation), donc portées par CompteService
	// et non par des annotations : les appelants internes construisent des requêtes
	// sans imputation, qui doivent rester valides.

	/** Nature de la dépense. Obligatoire sur une dépense saisie en trésorerie. */
	private Long typeDepenseId;

	/** Imputation directe à un véhicule. Exclusif de {@code missionId} et {@code nonImputable}. */
	private Long vehiculeId;

	/** Imputation à une mission ; le véhicule en est déduit puis figé. Exclusif des deux autres. */
	private Long missionId;

	/** Dépense assumée sans imputation (frais généraux). Exclusif des deux autres. */
	private Boolean nonImputable;
}
