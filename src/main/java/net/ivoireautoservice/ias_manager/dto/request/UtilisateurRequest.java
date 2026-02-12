package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UtilisateurRequest {

	@NotBlank(message = "Le nom est obligatoire")
	private String nom;

	@NotBlank(message = "Le prénom est obligatoire")
	private String prenom;

	@NotBlank(message = "L'email est obligatoire")
	@Email(message = "L'email doit être valide")
	private String email;

	private String adresse;
	private String telephone;

	@NotBlank(message = "Le mot de passe est obligatoire")
	private String password;
}