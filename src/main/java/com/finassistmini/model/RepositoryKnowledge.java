package com.finassistmini.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryKnowledge {

    private String readme;

    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    @Builder.Default
    private List<String> entryPoints = new ArrayList<>();

    @Builder.Default
    private List<String> importantClasses = new ArrayList<>();

    @Builder.Default
    private List<String> configuration = new ArrayList<>();

    @Builder.Default
    private List<String> detectedTechnologies = new ArrayList<>();
}