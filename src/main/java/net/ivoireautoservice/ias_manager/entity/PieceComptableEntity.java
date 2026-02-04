package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PIECES_COMPTABLES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"typeStatut", "partenaire"})
@EqualsAndHashCode(exclude = {"typeStatut", "partenaire"})
public class PieceComptableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String numProforma;

	private LocalDateTime dhmsCreationPiece;

	private String numFacture;

	private LocalDateTime dhmsFacture;

	private Long montantHt;

	private Float tva;

	private Long montantTtc;

	private LocalDate delaiLivraison;

	private LocalDate validite;

	private String objet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_statut_id", referencedColumnName = "id")
	private TypeStatutPieceComptableEntity typeStatut;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partenaire_id", referencedColumnName = "id")
	private PartenaireEntity partenaire;
}
