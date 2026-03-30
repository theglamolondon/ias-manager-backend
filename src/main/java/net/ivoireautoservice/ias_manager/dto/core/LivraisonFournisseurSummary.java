package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data @AllArgsConstructor @NoArgsConstructor @SuperBuilder @ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LivraisonFournisseurSummary extends LivraisonSummary {
	private String numero;
}
