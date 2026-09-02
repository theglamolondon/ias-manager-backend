package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.PhotoMissionTypeEnum;

@Entity
@Table(name = "photos_mission")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"mission", "media"})
@EqualsAndHashCode(callSuper = true, exclude = {"mission", "media"})
public class PhotoMissionEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private MissionEntity mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "media_id", nullable = false)
	private MediaEntity media;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", length = 30)
	private PhotoMissionTypeEnum type;
}
