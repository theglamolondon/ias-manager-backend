package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "COMPTES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
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

	@Column(name = "is_appro", nullable = false)
	private Boolean isAppro;
}
