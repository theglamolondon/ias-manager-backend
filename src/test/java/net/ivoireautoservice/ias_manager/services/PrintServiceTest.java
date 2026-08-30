package net.ivoireautoservice.ias_manager.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrintService — rendu Thymeleaf vers PDF")
class PrintServiceTest {

	@Mock
	private TemplateEngine templateEngine;

	@InjectMocks
	private PrintService service;

	@Test
	@DisplayName("le template est rendu avec les variables fournies puis converti en PDF")
	void generationNominale() {
		when(templateEngine.process(anyString(), any(IContext.class)))
				.thenReturn("<html><body><h1>Facture</h1></body></html>");

		byte[] pdf = service.generatePdf("pdf/factureProforma", Map.of("numero", "DA/01/79/1"));

		assertThat(pdf).isNotEmpty();
		assertThat(new String(pdf, 0, 5)).startsWith("%PDF");

		ArgumentCaptor<IContext> captor = ArgumentCaptor.forClass(IContext.class);
		verify(templateEngine).process(eq("pdf/factureProforma"), captor.capture());
		assertThat(captor.getValue().getVariable("numero")).isEqualTo("DA/01/79/1");
	}

	@Test
	@DisplayName("un HTML non conforme remonte une erreur explicite")
	void htmlInvalide() {
		when(templateEngine.process(anyString(), any(IContext.class)))
				.thenReturn("ceci n'est pas du XHTML <<<");

		assertThatThrownBy(() -> service.generatePdf("pdf/casse", Map.of()))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("generation du PDF");
	}

	@Test
	@DisplayName("un jeu de données vide reste imprimable")
	void donneesVides() {
		when(templateEngine.process(anyString(), any(Context.class)))
				.thenReturn("<html><body>vide</body></html>");

		assertThat(service.generatePdf("pdf/vide", Map.of())).isNotEmpty();
	}
}
