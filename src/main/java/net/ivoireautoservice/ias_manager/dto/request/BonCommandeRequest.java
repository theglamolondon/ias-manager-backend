package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BonCommandeRequest {

	private LocalDate dateCommande;
}
