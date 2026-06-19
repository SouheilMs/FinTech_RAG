package com.finassistmini.service;

import com.finassistmini.model.ParsedPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfParserService {

    public List<ParsedPage> parsePdf(Path filePath) {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ParsedPage> pages = new ArrayList<>();
            for (int pageIndex = 1; pageIndex <= document.getNumberOfPages(); pageIndex++) {
                stripper.setStartPage(pageIndex);
                stripper.setEndPage(pageIndex);
                String text = stripper.getText(document).trim();
                if (!text.isBlank()) {
                    pages.add(new ParsedPage(pageIndex, text));
                }
            }
            return pages;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse PDF '" + filePath.getFileName() + "'.", ex);
        }
    }
}
