package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.Valid;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureRequest {

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
	private Boolean factureClient;
	private FactureStatusEnum statut;
	private Long partenaireId;

	@Valid
	private List<LigneFactureRequest> items;
}