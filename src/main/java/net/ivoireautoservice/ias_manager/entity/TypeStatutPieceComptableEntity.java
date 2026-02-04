package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TYPES_STATUT_PIECE_COMPTABLE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeStatutPieceComptableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String libelle;
}
