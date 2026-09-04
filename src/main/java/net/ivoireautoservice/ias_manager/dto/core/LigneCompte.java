package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.LigneCompteOrigine;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LigneCompte {
	private Long id;
	private Long utilisateurId;
	private String utilisateurNom;
	private String utilisateurPrenom;
	private Long compteId;
	private String compteIntitule;
	private Long factureId;
	private CompteLigneType type;
	private LigneCompteOrigine origine;
	private LocalDateTime dhmsOperation;
	private String objet;
	private Long montant;
	private Long balanceAvant;
	private String observation;

	// Imputation analytique (dépenses uniquement)
	private Long typeDepenseId;
	private String typeDepenseLibelle;
	private Long vehiculeId;
	private String vehiculeImmatriculation;
	private Long missionId;
	private String codeMission;
}
