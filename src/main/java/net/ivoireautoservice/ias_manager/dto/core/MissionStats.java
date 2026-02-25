package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MissionStats {
	private long total;
	private long confirmees;
	private long enCours;
	private BigDecimal coutTotalConfirmees;
}
