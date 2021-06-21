package ir.sahab.sonar.gerrit.integrator.service;

import com.google.gerrit.extensions.api.changes.ReviewInput.RobotCommentInput;
import com.google.gerrit.extensions.client.Comment;
import ir.sahab.sonar.gerrit.integrator.dto.SonarSearchResult;
import ir.sahab.sonar.gerrit.integrator.dto.SonarSearchResult.Issue;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook.Branch.BranchType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SonarQubeService {
    private final RestTemplate restTemplate;
    private final String sonarAddress;

    public SonarQubeService(RestTemplateBuilder restTemplateBuilder,
                            @Value("${sonar.address}") String sonarAddress,
                            @Value("${sonar.token}") String sonarToken) {
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

    public Map<String, List<RobotCommentInput>> createGerritComments(SonarSearchResult searchResult,
            SonarWebHook webHook) throws UnsupportedEncodingException {
        if (searchResult.total == 0) {
            return Collections.emptyMap();
        }
        Map<String, List<RobotCommentInput>> comments = new HashMap<>();
        for (Issue issue : searchResult.issues) {
            RobotCommentInput comment = new RobotCommentInput();
            String urlEncodedRuleId = URLEncoder.encode(issue.rule, StandardCharsets.UTF_8.toString());
            String url = String.format("%s/coding_rules?open=%s&rule_key=%s", sonarAddress,
                    urlEncodedRuleId, urlEncodedRuleId);
            comment.message = issue.message + "\n\nRead more: " + url;
            comment.robotId = "SonarQube";
            comment.robotRunId = webHook.taskId;

            String type = webHook.branch.type == BranchType.BRANCH ? "branch" : "pullRequest";
            comment.url = String.format("%s/project/issues?id=%s&open=%s&%s=%s", sonarAddress, webHook.project.key,
                    issue.key, type, webHook.branch.name);

            if (issue.textRange != null) {
                comment.range = new Comment.Range();
                comment.range.startLine = issue.textRange.startLine;
                comment.range.endLine = issue.textRange.endLine;
                comment.range.startCharacter = issue.textRange.startOffset;
                comment.range.endCharacter = issue.textRange.endOffset;
            } else if (issue.line != null) {
                comment.line = issue.line;
            }

            String filePath = issue.component.substring(issue.component.lastIndexOf(':') + 1);
            comments.computeIfAbsent(filePath, path -> new ArrayList<>());
            comments.get(filePath).add(comment);
        }
        return comments;
    }
}
