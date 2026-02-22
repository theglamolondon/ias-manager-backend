package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeDepenseRequest {
	private Long id;
	private String libelle;
}
