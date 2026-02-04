package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LignePieceComptableRequest {

	private String reference;
	private Long qte;
	private Long prixUnitaire;
	private Float remise;
	private String designation;
	private Long montantHt;
	private Long produitId;
}
