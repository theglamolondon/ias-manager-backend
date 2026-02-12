package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntreeStock {
	private LivraisonFournisseur livraison;
	private List<EntreeProduit> entrees;
}