package net.ivoireautoservice.ias_manager.dto;

import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagedResponse — projection d'une Page Spring Data")
class PagedResponseTest {

	@Test
	@DisplayName("recopie contenu, index, taille et totaux")
	void of_pagePleine() {
		var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

		PagedResponse<String> response = PagedResponse.of(page);

		assertThat(response.getContent()).containsExactly("a", "b");
		assertThat(response.getPage()).isZero();
		assertThat(response.getSize()).isEqualTo(2);
		assertThat(response.getTotalElements()).isEqualTo(5);
		assertThat(response.getTotalPages()).isEqualTo(3);
		assertThat(response.isLastPage()).isFalse();
	}

	@Test
	@DisplayName("marque la dernière page")
	void of_dernierePage() {
		var page = new PageImpl<>(List.of("e"), PageRequest.of(2, 2), 5);

		assertThat(PagedResponse.of(page).isLastPage()).isTrue();
	}

	@Test
	@DisplayName("gère une page vide")
	void of_pageVide() {
		var page = new PageImpl<String>(List.of(), PageRequest.of(0, 10), 0);

		PagedResponse<String> response = PagedResponse.of(page);

		assertThat(response.getContent()).isEmpty();
		assertThat(response.getTotalElements()).isZero();
		assertThat(response.getTotalPages()).isZero();
		assertThat(response.isLastPage()).isTrue();
	}
}
