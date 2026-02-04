package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class LignePieceComptable {
	private Long id;
	private String reference;
	private Long qte;
	private Long prixUnitaire;
	private Float remise;
	private String designation;
	private Long montantHt;
	private Long pieceComptableId;
	private Long produitId;
	private String produitDesignation;
}
