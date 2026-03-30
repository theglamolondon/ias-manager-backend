package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COMPTES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"utilisateurs", "logo"})
@EqualsAndHashCode(callSuper = true, exclude = {"utilisateurs", "logo"})
public class CompteEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String intitule;

	@Column(nullable = false, unique = true)
	private String numero;

	private String description;

	@Column(nullable = false)
	private Long balance;

	@Column(name = "can_appro", nullable = false)
	@Builder.Default
	private Boolean canAppro = false;

	@Column(name = "can_be_negative", nullable = false)
	@Builder.Default
	private Boolean canBeNegative = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "logo_id")
	private MediaEntity logo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "utilisateur_id")
	private Utilisateur manager;

	@OneToMany(mappedBy = "compte", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<CompteUtilisateurEntity> utilisateurs = new ArrayList<>();
}
