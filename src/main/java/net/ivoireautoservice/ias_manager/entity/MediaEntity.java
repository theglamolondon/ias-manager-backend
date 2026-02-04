package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MEDIAS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(nullable = false)
	private String originalFilename;

	@Column(nullable = false)
	private String storedFilename;

	@Column(nullable = false)
	private String contentType;

	private Long size;
}
