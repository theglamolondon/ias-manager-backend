package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TYPES_INTERVENTION")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeInterventionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String libelle;
}
