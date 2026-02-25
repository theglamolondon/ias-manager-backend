package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InterventionStats {
	private long total;
	private long enCours;
	private long vehiculesConcernes;
	private long coutTotal;
}
