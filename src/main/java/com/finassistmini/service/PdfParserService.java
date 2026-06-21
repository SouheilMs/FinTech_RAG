package com.finassistmini.service;

import com.finassistmini.model.ParsedPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfParserService {

    private static final Logger log = LoggerFactory.getLogger(PdfParserService.class);

    public List<ParsedPage> parse(Path pdfPath) throws IOException {
        List<ParsedPage> pages = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int total = doc.getNumberOfPages();

            for (int i = 1; i <= total; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc).strip();
                if (!text.isBlank()) {
                    pages.add(new ParsedPage(i, text));
                }
            }
        }
        log.info("Parsed {} non-empty pages from '{}'", pages.size(), pdfPath.getFileName());
        return pages;
    }
}