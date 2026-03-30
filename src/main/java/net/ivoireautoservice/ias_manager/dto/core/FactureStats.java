package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureStats {
	private long total;
	private long enAttente;
	private long annulees;
	private long payees;
	private long impayees;
	private long montantTotal;
	private long montantTotalEnAttente;
	private long montantTotalPaye;
	private long montantTotalImpaye;
}
