package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Compte {
	private Long id;
	private String intitule;
	private String numero;
	private String description;
	private Long balance;
	private Boolean canAppro;
	private UtilisateurDto manager;
	private List<CompteUtilisateur> utilisateurs;
}
