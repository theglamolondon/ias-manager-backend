package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonClientRequest {

	private String objet;

	private LocalDateTime dhmsLivraison;
}
