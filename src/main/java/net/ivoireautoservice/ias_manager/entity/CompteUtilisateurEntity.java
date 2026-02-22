package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "COMPTE_UTILISATEURS", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"compte_id", "utilisateur_id"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"compte", "utilisateur"})
@EqualsAndHashCode(callSuper = true, exclude = {"compte", "utilisateur"})
public class CompteUtilisateurEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "compte_id", nullable = false)
	private CompteEntity compte;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "utilisateur_id", nullable = false)
	private Utilisateur utilisateur;

	@Column(name = "can_appro", nullable = false)
	@Builder.Default
	private Boolean canAppro = false;
}
