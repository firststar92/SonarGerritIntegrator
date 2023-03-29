package ir.sahab.sonar.gerrit.integrator.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import ir.sahab.sonar.gerrit.integrator.dto.GerritPatchSet;
import ir.sahab.sonar.gerrit.integrator.dto.QualityGate;
import ir.sahab.sonar.gerrit.integrator.dto.QualityGate.QualityGateStatus;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook.AnalysisStatus;
import ir.sahab.sonar.gerrit.integrator.exception.InvalidRequestException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GerritService {

    private static final String invalidGerritIdErrorMessage = "Gerrit ID is not a valid. "
            + "It should be in form of <gerrit-cl-number>-<gerrit-patch-set-number>: '%s'";

    private final RestTemplate restTemplate;
    private final String gerritAddress;

    public GerritService(RestTemplateBuilder restTemplateBuilder,
                         @Value("${gerrit.address}") String gerritAddress,
                         @Value("${gerrit.user}") String gerritUser,
                         @Value("${gerrit.token}") String gerritToken) {
        this.restTemplate = restTemplateBuilder.basicAuthentication(gerritUser, gerritToken).messageConverters(
                new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                        .serializationInclusion(JsonInclude.Include.NON_NULL)
                        .visibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .build())).build();
        this.gerritAddress = gerritAddress;
    }

    @VisibleForTesting
    public RestTemplate getRestTemplate() {
        return restTemplate;
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

    private static String humanizeSnakeCase(String name) {
        return StringUtils.capitalize(StringUtils.join(
                StringUtils.split(StringUtils.removeStart(name.toLowerCase(Locale.ENGLISH), "new_"), '_'),
                StringUtils.SPACE));
    }

    public static GerritPatchSet getPatchSet(SonarWebHook webHook) {
        final String gerritPropKey = "sonar.analysis.gerritId";
        String gerritId = webHook.properties.get(gerritPropKey);
        if (gerritId == null) {
            throw new ResponseStatusException(HttpStatus.OK,
                    "Not a gerrit review. '" + gerritPropKey + "' does not exists!");
        }
        String[] idParts = gerritId.split("-", -1);
        if (idParts.length != 2) {
            throw new InvalidRequestException(String.format(invalidGerritIdErrorMessage, gerritId));
        }

        try {
            return new GerritPatchSet(Integer.parseInt(idParts[0]), Integer.parseInt(idParts[1]));
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(String.format(invalidGerritIdErrorMessage, gerritId));
        }
    }

    private static String generateGerritMessage(SonarWebHook webHook) {
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
            String metricName = humanizeSnakeCase(condition.metric);
            message.append(getConditionEmoji(condition))
                    .append(' ')
                    .append(metricName)
                    .append("(not ")
                    .append(humanizeSnakeCase(condition.operator.name()).toLowerCase(Locale.ENGLISH))
                    .append(' ')
                    .append(condition.errorThreshold)
                    .append(condition.value != null ? ", value: " + condition.value : ", not-computed")
                    .append(")\n");
        }

        message.append('\n').append("Url: ").append(webHook.branch.url);

        return message.toString();
    }

    public void sendGerritReview(Map<String, List<ReviewInput.RobotCommentInput>> comments, GerritPatchSet patchSet,
                                 SonarWebHook webHook) {
        ReviewInput review = webHook.qualityGate.status == QualityGateStatus.OK ?
                ReviewInput.recommend() : ReviewInput.dislike();
        review.message = generateGerritMessage(webHook);
        review.tag = "autogenerated:sonarqube";
        review.robotComments = comments;

        String url = String.format("%s/a/changes/%d/revisions/%d/review", gerritAddress, patchSet.changeId,
                patchSet.patchSetNumber);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, review, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY,
                    "Gerrit responded: " + response.getStatusCode());
        }

    }
}
