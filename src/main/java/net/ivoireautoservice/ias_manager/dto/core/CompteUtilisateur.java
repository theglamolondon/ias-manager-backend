package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompteUtilisateur {
	private Long id;
	private Long utilisateurId;
	private String utilisateurNom;
	private String utilisateurPrenom;
	private String utilisateurEmail;
	private Boolean canAppro;
	private Boolean canSettle;
}
