package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonFournisseurRequest {

	private String numero;

	private LocalDateTime dhmsLivraison;

	private Long factureId;

	private List<EntreeProduitRequest> items;
}
