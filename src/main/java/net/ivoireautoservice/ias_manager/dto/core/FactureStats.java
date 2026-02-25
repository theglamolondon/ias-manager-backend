package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureStats {
	private long total;
	private long enAttente;
	private long payees;
	private long montantTotalPaye;
	private long montantTotal;
}
