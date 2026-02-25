package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonFournisseurStats {
	private long total;
	private long avecFacture;
	private long sansFacture;
	private long produitsEntres;
}
