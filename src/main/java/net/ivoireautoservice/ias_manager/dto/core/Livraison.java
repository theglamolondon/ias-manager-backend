package net.ivoireautoservice.ias_manager.dto.core;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@Type(value = LivraisonClient.class, name = "CLIENT"),
		@Type(value = LivraisonFournisseur.class, name = "FOURNISSEUR")
})
public class Livraison {
	private Long id;
	private LocalDateTime dhmsLivraison;
	private Long factureId;
	private String factureNumProforma;
	private String factureType;
	private Long createdById;
	private String createdByNom;
}
