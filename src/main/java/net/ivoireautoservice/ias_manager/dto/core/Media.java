package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Media {
	private String id;
	private String originalFilename;
	private String contentType;
	private Long size;
	private String url;
}
