package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

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
	private LocalDateTime dhmsOperation;
	private String objet;
	private Long montant;
	private Long balanceAvant;
	private String observation;
}
