package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Dépense de trésorerie imputée à un véhicule, telle qu'affichée sur sa fiche.
 * Vue en lecture d'une ligne de compte d'origine manuelle — la seule qui porte
 * la valeur analytique de la dépense.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepenseImputee {
	private Long id;
	private String libelle;
	private Long montant;
	private LocalDateTime dhmsOperation;
	private Long typeDepenseId;
	private String typeDepenseLibelle;
	private Long compteId;
	private String compteIntitule;
	private Long missionId;
	private String codeMission;
}
