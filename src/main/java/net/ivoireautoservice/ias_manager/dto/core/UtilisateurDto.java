package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UtilisateurDto {
	private Long id;
	private String nom;
	private String prenom;
	private String email;
	private String adresse;
	private String telephone;
	private String role;
	private Boolean hasChangePassword;
	private Long employeId;
}
