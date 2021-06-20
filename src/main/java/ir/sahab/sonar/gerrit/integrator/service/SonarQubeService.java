package ir.sahab.sonar.gerrit.integrator.service;

import ir.sahab.sonar.gerrit.integrator.dto.SonarSearchResult;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook.Branch.BranchType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SonarQubeService {
    private final RestTemplate restTemplate;
    private final String sonarAddress;

    public SonarQubeService(RestTemplateBuilder restTemplateBuilder,
                            @Value("sonar.address") String sonarAddress,
                            @Value("sonar.token") String sonarToken) {
        this.restTemplate = restTemplateBuilder.basicAuthentication(sonarToken, "").build();
        this.sonarAddress = sonarAddress;
    }

    public SonarSearchResult getIssuesOf(SonarWebHook webHook) {
        String type = webHook.branch.type == BranchType.BRANCH ? "branch" : "pullRequest";
        final String url = String.format("%s/api/issues/search?%s=%s&componentKeys=%s", sonarAddress, type,
                webHook.branch.name, webHook.project.key);
        ResponseEntity<SonarSearchResult> response = restTemplate.getForEntity(url, SonarSearchResult.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY,
                    "SonarQube responded: " + response.getStatusCode());
        }
        return response.getBody();
    }
}
