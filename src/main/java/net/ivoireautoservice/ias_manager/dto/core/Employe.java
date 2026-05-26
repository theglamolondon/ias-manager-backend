package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Employe {
	private Long id;
	private String matricule;
	private String nom;
	private String prenoms;
	private String photo;
	private LocalDate dateNaissance;
	private String rib;
	private String numeroCnps;
	private LocalDate dateEmbauche;
	private LocalDate dateDepart;
	private String telephone1;
	private String telephone2;
	private String email;
	private String lieuNaissance;
	private Long serviceId;
	private String serviceLibelle;

	private Boolean isChauffeur;
	private Long chauffeurId;
	private String numeroPermis;
	private LocalDate expDatePermis;
	private String typePermis;
}
