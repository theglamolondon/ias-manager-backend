package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureNatureEnum;
import net.ivoireautoservice.ias_manager.enums.FactureTypeEnum;

import java.time.LocalDate;

@Entity
@Table(name = "FACTURES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"partenaire"})
@EqualsAndHashCode(callSuper = true, exclude = {"partenaire"})
public class FactureEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String numProforma;

	private String numFacture;

	private Long montantHt;

	private Float tva;

	private Long montantTtc;

	private LocalDate delaiLivraison;

	private LocalDate validite;

	private String objet;

	private Boolean factureClient;

	@Enumerated(EnumType.ORDINAL)
	private FactureStatusEnum statut;

	@Enumerated(EnumType.STRING)
	private FactureNatureEnum nature;

	@Enumerated(EnumType.STRING)
	private FactureTypeEnum type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partenaire_id", referencedColumnName = "id")
	private PartenaireEntity partenaire;
}