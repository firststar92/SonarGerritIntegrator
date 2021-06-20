package ir.sahab.sonar.gerrit.integrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.WebClientRestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SonarGerritIntegratorApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void testPost() throws URISyntaxException, IOException {
		Path path = Paths.get(WebHookTest.class.getResource("/webhook.json").toURI());
		String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		RequestEntity e = new RequestEntity(data, headers, null, null, null);
		ResponseEntity<String> response = restTemplate.exchange("/", HttpMethod.POST, e, String.class);
		System.out.println(response.getBody());
	}
}
