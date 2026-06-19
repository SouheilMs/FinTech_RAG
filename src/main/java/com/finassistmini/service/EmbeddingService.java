package com.finassistmini.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Double> embedText(String text) {
        float[] vector = embeddingModel.embed(text);
        List<Double> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add((double) value);
        }
        return values;
    }

    public List<List<Double>> embedTexts(List<String> texts) {
        return texts.stream()
                .map(this::embedText)
                .toList();
    }
}
