package ir.sahab.sonar.gerrit.integrator;

import com.google.gerrit.extensions.api.changes.ReviewInput;
import ir.sahab.sonar.gerrit.integrator.dto.GerritPatchSet;
import ir.sahab.sonar.gerrit.integrator.dto.SonarSearchResult;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.service.GerritService;
import ir.sahab.sonar.gerrit.integrator.service.SonarQubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@RestController
public class Controller {
    private final GerritService gerritService;
    private final SonarQubeService sonarQubeService;

    @Autowired
    public Controller(GerritService gerritService, SonarQubeService sonarQubeService) {
        this.gerritService = gerritService;
        this.sonarQubeService = sonarQubeService;
    }

    @PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void handleWebHook(@RequestBody @Valid SonarWebHook webHook) throws UnsupportedEncodingException {
        GerritPatchSet patchSet = GerritService.getPatchSet(webHook);
        SonarSearchResult sonarIssues = sonarQubeService.getIssuesOf(webHook);
        Map<String, List<ReviewInput.RobotCommentInput>> comments = sonarQubeService.createGerritComments(sonarIssues,
                webHook);
        gerritService.sendGerritReview(comments, patchSet, webHook);
    }
}
