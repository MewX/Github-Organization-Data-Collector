package org.mewx.github.collector.model;

import java.sql.Timestamp;
import java.util.SortedMap;
import java.util.TreeMap;

public class ProjectTechStack {
    public String projectName;
    public SortedMap<String, Double> languages;

    public ProjectTechStack(String name, String languages) {
        this.projectName = name; // remember to remove the owner name
        this.languages = parseLanguages(languages);
    }

    private SortedMap<String, Double> parseLanguages(String languages) {
        SortedMap<String, Double> temp = new TreeMap<>();
        String[] lines = languages.split("\\n");
        for (String line : lines) {
            String[] segments = line.split("%\\s+");
            if (segments.length == 2) {
                temp.put(segments[1].trim(), Double.valueOf(segments[0].trim()));
            }
        }
        return temp;
    }

    public void add(ProjectTechStack projectTechStack) {
        for (String language : projectTechStack.languages.keySet()) {
            languages.put(language,
                    languages.getOrDefault(language, 0.0) + projectTechStack.languages.get(language));
        }
    }

}
