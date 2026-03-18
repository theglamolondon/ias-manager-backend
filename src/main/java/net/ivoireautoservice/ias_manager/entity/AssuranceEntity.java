package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ASSURANCES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"logo"})
@EqualsAndHashCode(callSuper = true, exclude = {"logo"})
public class AssuranceEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String libelle;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "logo_id")
	private MediaEntity logo;
}
