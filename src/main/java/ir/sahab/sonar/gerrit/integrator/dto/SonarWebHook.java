package ir.sahab.sonar.gerrit.integrator.dto;

import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class SonarWebHook {
    public String serverUrl;
    public String taskId;

    @NotNull
    public AnalysisStatus status;
    public Instant analysedAt;
    public String revision;
    public Instant changedAt;
    public Project project;

    @NotNull
    public Branch branch;

    @NotNull
    public QualityGate qualityGate;

    @NotNull
    public Map<String, String> properties;

    public enum AnalysisStatus {
        SUCCESS, FAILED
    }

    public static class Project {
        @NotNull
        public String key;
        public String name;
        public String url;
    }

    public static class Branch {
        @NotNull
        public String name;

        @NotNull
        public BranchType type;

        public Boolean isMain;

        @NotNull
        public String url;

        public enum BranchType {
            PULL_REQUEST, BRANCH
        }
    }

}
