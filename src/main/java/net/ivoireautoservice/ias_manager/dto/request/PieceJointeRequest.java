package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PieceJointeRequest {

	@NotNull
	private PieceJointeOwnerTypeEnum ownerType;

	@NotNull
	private Long ownerId;
}
