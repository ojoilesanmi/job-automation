package com.jobagent.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Slf4j
@Service
public class PdfExportService {

    public byte[] generatePdf(String htmlContent) {
        try {
            String fullHtml = buildHtmlDocument(htmlContent);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new PdfRendererBuilder()
                    .withHtmlContent(fullHtml, "/")
                    .toStream(baos)
                    .run();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String buildHtmlDocument(String content) {
        String escaped = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "</p><p>");
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Helvetica Neue', Arial, sans-serif; font-size: 11pt; line-height: 1.6; color: #333; }
                    h1 { font-size: 18pt; color: #1a1a1a; border-bottom: 2px solid #2563eb; padding-bottom: 8px; }
                    p { margin: 8px 0; }
                    .header { text-align: center; margin-bottom: 24px; }
                </style>
            </head>
            <body>
                <div class="header"><h1>Cover Letter</h1></div>
                <p>%s</p>
            </body>
            </html>
            """.formatted(escaped);
    }
}
