package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "medias")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MediaEntity extends AuditableEntity {

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
