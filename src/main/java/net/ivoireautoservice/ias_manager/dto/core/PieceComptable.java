package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class PieceComptable {
	private Long id;
	private String numProforma;
	private LocalDateTime dhmsCreationPiece;
	private String numFacture;
	private LocalDateTime dhmsFacture;
	private Long montantHt;
	private Float tva;
	private Long montantTtc;
	private LocalDate delaiLivraison;
	private LocalDate validite;
	private String objet;
	private Long typeStatutId;
	private String typeStatutLibelle;
	private Long partenaireId;
	private String partenaireRaisonSociale;
}
