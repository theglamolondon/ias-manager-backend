package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @SuperBuilder @ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LivraisonClient extends Livraison {
	private List<SortieProduit> sorties;
}
