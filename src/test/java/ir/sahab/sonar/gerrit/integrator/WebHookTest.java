package ir.sahab.sonar.gerrit.integrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.sahab.sonar.gerrit.integrator.dto.SonarWebHook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@SpringBootTest
public class WebHookTest {

    @Autowired
    public ObjectMapper objectMapper;

    @Test
    void test() throws IOException, URISyntaxException {
        Path path = Paths.get(WebHookTest.class.getResource("/webhook.json").toURI());
        SonarWebHook webHook = objectMapper.reader().readValue(Files.readAllBytes(path), SonarWebHook.class);
    }
}
