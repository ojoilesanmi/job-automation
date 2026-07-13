package com.jobagent.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class DocxExportService {

    public byte[] generateDocx(String content, String title) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            if (title != null && !title.isBlank()) {
                XWPFParagraph titleParagraph = document.createParagraph();
                titleParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun titleRun = titleParagraph.createRun();
                titleRun.setText(title);
                titleRun.setBold(true);
                titleRun.setFontSize(18);
                titleRun.setFontFamily("Arial");

                addEmptyParagraph(document);
            }

            String[] paragraphs = content.split("\n");
            for (String para : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.LEFT);

                if (para.isBlank()) {
                    continue;
                }

                XWPFRun run = paragraph.createRun();
                run.setText(para.trim());
                run.setFontSize(11);
                run.setFontFamily("Arial");
            }

            document.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("DOCX generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }

    private void addEmptyParagraph(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("");
    }
}
