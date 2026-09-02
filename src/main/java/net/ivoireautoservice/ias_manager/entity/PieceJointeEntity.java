package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;

@Entity
@Table(name = "pieces_jointes", indexes = {
	@Index(name = "idx_pj_owner", columnList = "owner_type,owner_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "media")
@EqualsAndHashCode(callSuper = true, exclude = "media")
public class PieceJointeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "owner_type", nullable = false)
	private PieceJointeOwnerTypeEnum ownerType;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "media_id", referencedColumnName = "id", nullable = false)
	private MediaEntity media;
}
