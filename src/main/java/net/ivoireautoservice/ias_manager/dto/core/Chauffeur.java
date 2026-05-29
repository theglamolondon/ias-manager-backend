package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.StatutChauffeurEnum;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Chauffeur {
	private Long id;
	private Long employeId;
	private String employeMatricule;
	private String employeNom;
	private String employePrenoms;
	private String numeroPermis;
	private LocalDate expDatePermis;
	private LocalDate expDatePermisC;
	private LocalDate expDatePermisD;
	private LocalDate expDatePermisE;
	private String typePermis;
	private StatutChauffeurEnum statut;
}
