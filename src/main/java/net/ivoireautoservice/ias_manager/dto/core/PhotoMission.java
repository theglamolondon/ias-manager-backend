package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.PhotoMissionTypeEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhotoMission {
	private Long id;
	private PhotoMissionTypeEnum type;
	private Media media;
}
