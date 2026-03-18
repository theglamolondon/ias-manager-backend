package net.ivoireautoservice.ias_manager.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrintService {

	private final TemplateEngine templateEngine;

	/**
	 * Genere un PDF a partir d'un template Thymeleaf et d'un jeu de donnees.
	 *
	 * @param templateName nom du template sans extension (ex: "pdf/factureProforma")
	 * @param data         donnees injectees dans le template
	 * @return le PDF sous forme de byte[]
	 */
	public byte[] generatePdf(String templateName, Map<String, Object> data) {
		Context context = new Context();
		context.setVariables(data);

		String html = templateEngine.process(templateName, context);

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(html, "/");
			builder.toStream(os);
			builder.run();
			return os.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Erreur lors de la generation du PDF : " + e.getMessage(), e);
		}
	}
}
