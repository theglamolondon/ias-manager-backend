package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeRequest {

	@NotBlank(message = "Le matricule est obligatoire")
	private String matricule;

	@NotBlank(message = "Le nom est obligatoire")
	private String nom;

	@NotBlank(message = "Les prénoms sont obligatoires")
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

	private Boolean isChauffeur;
	private String numeroPermis;
	private LocalDate expDatePermis;
	private String typePermis;
}
