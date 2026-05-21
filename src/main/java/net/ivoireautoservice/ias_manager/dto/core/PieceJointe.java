package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;

import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class PieceJointe {
	private Long id;
	private LocalDateTime createdAt;
	private PieceJointeOwnerTypeEnum ownerType;
	private Long ownerId;
	private String mediaId;
	private String originalFilename;
	private String contentType;
	private Long size;
}
