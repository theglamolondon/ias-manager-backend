package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "SITES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SiteEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "raison_sociale")
	private String raisonSociale;

	private String logo;

	private String devise;

	@Column(name = "sup_is_interieur", precision = 19, scale = 2)
	private BigDecimal supIsInterieur;

	@Column(name = "sup_is_exterieur", precision = 19, scale = 2)
	private BigDecimal supIsExterieur;
}
