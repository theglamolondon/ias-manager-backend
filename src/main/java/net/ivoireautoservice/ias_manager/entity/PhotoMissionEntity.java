package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PHOTOS_MISSION")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"mission", "media"})
@EqualsAndHashCode(exclude = {"mission", "media"})
public class PhotoMissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private MissionEntity mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "media_id", nullable = false)
	private MediaEntity media;
}
