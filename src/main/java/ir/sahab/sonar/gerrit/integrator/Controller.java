package ir.sahab.sonar.gerrit.integrator;

import ir.sahab.sonar.gerrit.integrator.dto.GerritPatchSet;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.service.GerritService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class Controller {
    private final GerritService gerritService;

    @Autowired
    public Controller(GerritService gerritService) {
        this.gerritService = gerritService;
    }

    @PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String handleWebHook(@RequestBody @Valid SonarWebHook webHook) {
        GerritPatchSet patchSet = gerritService.getPatchSet(webHook);
        return gerritService.generateGerritMessage(webHook);
    }
}
