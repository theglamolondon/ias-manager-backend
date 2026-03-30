package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.TypePartenaireEnum;

@Entity
@Table(name = "PARTENAIRES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PartenaireEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String raisonSociale;

	private String numRc;
	private String numCc;
	private String telephone1;
	private String telephone2;
	private String email1;
	private String email2;
	private String adresse;

	@Enumerated(EnumType.STRING)
	private TypePartenaireEnum type;

	@Column(nullable = false)
	private Boolean isClient;

	@Column(nullable = false)
	private Boolean isFournisseur;
}
