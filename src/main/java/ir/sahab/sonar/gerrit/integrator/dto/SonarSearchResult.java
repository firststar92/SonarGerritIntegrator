package ir.sahab.sonar.gerrit.integrator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarSearchResult {
    @NotNull
    public Integer total;

    @NotNull
    public List<Issue> issues;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        public Integer line;
        public TextRange textRange;

        @NotNull
        public String key;

        @NotNull
        public String rule;

        @NotNull
        public String severity;

        @NotNull
        public String component;

        @NotNull
        public String message;
    }

    public static class TextRange {
        @NotNull
        public Integer startLine;

        @NotNull
        public Integer endLine;

        @NotNull
        public Integer startOffset;

        @NotNull
        public Integer endOffset;
    }
}
