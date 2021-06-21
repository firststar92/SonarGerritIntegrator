package ir.sahab.sonar.gerrit.integrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import ir.sahab.sonar.gerrit.integrator.dto.SonarSearchResult;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import ir.sahab.sonar.gerrit.integrator.service.SonarQubeService;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class WebHookTest {

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    public SonarQubeService service;

    @Test
    void test() throws IOException, URISyntaxException {
        Path path = Paths.get(WebHookTest.class.getResource("/response.json").toURI());
        SonarSearchResult search = objectMapper.reader().readValue(Files.readAllBytes(path), SonarSearchResult.class);

        path = Paths.get(WebHookTest.class.getResource("/webhook.json").toURI());
        SonarWebHook webHook = objectMapper.reader().readValue(Files.readAllBytes(path), SonarWebHook.class);

        Map<String, List<ReviewInput.RobotCommentInput>> map = service.createGerritComments(search, webHook);

        System.out.println(new ReflectionToStringBuilder(map.entrySet().toArray(), new MultilineRecursiveToStringStyle()).toString());

    }
}
