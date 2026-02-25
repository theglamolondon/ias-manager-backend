package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChauffeurStats {
	private long total;
	private long permisValides;
	private long permisExpirentBientot;
	private long permisExpires;
}
