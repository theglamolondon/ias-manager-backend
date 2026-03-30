package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentVehicule {
	private Long id;
	private String label;
	private Media media;
	private LocalDateTime createdAt;
}
