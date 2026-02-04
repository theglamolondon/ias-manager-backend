package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonFournisseurRequest {

	private String numero;

	private LocalDateTime dhmsLivraison;
}
