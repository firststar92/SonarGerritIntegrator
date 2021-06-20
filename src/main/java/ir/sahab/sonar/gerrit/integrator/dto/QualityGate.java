package ir.sahab.sonar.gerrit.integrator.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

public class QualityGate {
    @NotNull
    public String name;

    @NotNull
    public QualityGateStatus status;

    @NotNull
    public List<Condition> conditions;

    public enum QualityGateStatus {
        OK, WARN, ERROR
    }

    public static class Condition {
        @NotNull
        public String metric;

        @NotNull
        public Condition.Operator operator;

        @NotNull
        public Condition.ConditionStatus status;

        @NotNull
        public String errorThreshold;

        public String value;

        public enum Operator {
            GREATER_THAN, LESS_THAN
        }

        public enum ConditionStatus {
            NO_VALUE, OK, ERROR
        }
    }
}
