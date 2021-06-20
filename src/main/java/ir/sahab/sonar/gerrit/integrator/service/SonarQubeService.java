package ir.sahab.sonar.gerrit.integrator.service;

import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook.Branch.BranchType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public void getIssuesOf(SonarWebHook webHook) {
        String type = webHook.branch.type == BranchType.BRANCH ? "branch" : "pullRequest";
        final String url = String.format("%s/api/issues/search?%s=%s&componentKeys=%s", sonarAddress, type,
                webHook.branch.name, webHook.project.key);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        System.out.println(response.getBody());
    }
}
