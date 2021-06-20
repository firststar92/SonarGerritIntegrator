package ir.sahab.sonar.gerrit.integrator.service;

import ir.sahab.sonar.gerrit.integrator.dto.GerritPatchSet;
import ir.sahab.sonar.gerrit.integrator.dto.QualityGate;
import ir.sahab.sonar.gerrit.integrator.dto.QualityGate.Condition.ConditionStatus;
import ir.sahab.sonar.gerrit.integrator.dto.QualityGate.QualityGateStatus;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook.AnalysisStatus;
import ir.sahab.sonar.gerrit.integrator.exception.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class GerritService {
    public GerritPatchSet getPatchSet(SonarWebHook webHook) {
        final String gerritPropKey = "sonar.analysis.gerritId";
        String gerritId = webHook.properties.get(gerritPropKey);
        if (gerritId == null) {
            throw new InvalidRequestException("WebHook properties does not have '" + gerritPropKey + "'!");
        }
        String[] IdParts = gerritId.split("-");
        if (IdParts.length != 2) {
            throw new InvalidRequestException("'" + gerritId + "' is not a valid gerrit ID!");
        }

        try {
            return new GerritPatchSet(Integer.parseInt(IdParts[0]), Integer.parseInt(IdParts[1]));
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("'" + gerritId + "' contains not integer parts!");
        }

    }

    public String generateGerritMessage(SonarWebHook webHook) {
        if (webHook.status == AnalysisStatus.FAILED) {
            return "🛑 Analysis failed!";
        }

        StringBuilder message = new StringBuilder();
        if (webHook.qualityGate.status == QualityGateStatus.OK) {
            message.append("✅ Quality gate passed.\n\n");
        } else {
            message.append("❌ Quality gate failed!\n\n");
        }

        for (QualityGate.Condition condition : webHook.qualityGate.conditions) {
            String metricName = humanizeKebabCase(condition.metric);
            message.append(getConditionEmoji(condition))
                    .append(' ')
                    .append(metricName)
                    .append("(not ")
                    .append(humanizeKebabCase(condition.operator.name()).toLowerCase(Locale.ENGLISH))
                    .append(' ')
                    .append(condition.errorThreshold)
                    .append(condition.value != null ? ", value: " + condition.value : ", not-computed")
                    .append(")\n");
        }

        message.append('\n').append("Url: ").append(webHook.branch.url);

        return message.toString();
    }

    private static String getConditionEmoji(QualityGate.Condition condition) {
        switch (condition.status) {
            case OK:
                return "☑️";
            case ERROR:
                return "⭕";
            default:
                return "⏹️";
        }
    }

    private static String humanizeKebabCase(String name) {
        return StringUtils.capitalize(StringUtils.join(
                        StringUtils.split(StringUtils.removeStart(name.toLowerCase(Locale.ENGLISH), "new_"), '_'),
                        StringUtils.SPACE));
    }
}
