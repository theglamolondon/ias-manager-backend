package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.Set;

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
	/** Rôle principal — conservé pour rétro-compatibilité (mono-rôle). */
	private String role;
	private Boolean hasChangePassword;
	private Long employeId;

	/** Rôles effectifs (directs + hérités des groupes). */
	private Set<String> roles;
	/** Groupes auxquels l'utilisateur appartient. */
	private Set<String> groupes;
	/** Permissions effectives, à plat. */
	private Set<String> permissions;
}
