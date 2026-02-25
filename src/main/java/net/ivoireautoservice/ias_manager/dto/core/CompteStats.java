package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompteStats {
	private long total;
	private long balanceTotale;
	private long totalSoldesPositifs;
	private long totalSoldesNegatifs;
}
