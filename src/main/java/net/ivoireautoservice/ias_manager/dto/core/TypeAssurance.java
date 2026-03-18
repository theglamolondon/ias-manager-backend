package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class TypeAssurance {
	private Long id;
	private String libelle;
	private String contact1;
	private String contact2;
}
