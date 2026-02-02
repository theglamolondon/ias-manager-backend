package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Intervention {
	private Long id;
	private LocalDate dhmsDebut;
	private LocalDate dhmsFin;
	private String objet;
	private String details;
	private Long cout;
	private Long typeInterventionId;
	private String typeInterventionLibelle;
	private Long vehiculeId;
	private String vehiculeImmatriculation;
}
