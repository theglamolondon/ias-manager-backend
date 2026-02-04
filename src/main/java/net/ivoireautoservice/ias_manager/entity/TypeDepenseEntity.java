package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TYPES_DEPENSE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeDepenseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String libelle;
}
