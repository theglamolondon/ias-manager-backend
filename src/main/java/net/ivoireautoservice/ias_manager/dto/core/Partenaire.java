package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class Partenaire {
	private Long id;
	private String raisonSociale;
	private String numRc;
	private String numCc;
	private String telephone1;
	private String telephone2;
	private String email1;
	private String email2;
	private Boolean isClient;
	private Boolean isFournisseur;
}
