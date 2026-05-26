package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartenaireStats {
	private long total;
	private long clients;
	private long fournisseurs;
}
