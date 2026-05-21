package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BonCommandeRequest {

	private LocalDate dateCommande;
	private LocalDate validite;
	private String objet;
	private Long montantHt;
	private Float tva;
	private Long montantTtc;
	private Long partenaireId;

	@Valid
	private List<LigneBonCommandeRequest> items;
}
