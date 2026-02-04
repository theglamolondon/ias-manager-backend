package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PieceComptableRequest {

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
	private Long partenaireId;
}
