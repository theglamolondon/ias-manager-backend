package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProduitStats {
	private long total;
	private long enRuptureDeStock;
	private long valeurTotaleStock;
}
