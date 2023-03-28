package ir.sahab.sonar.gerrit.integrator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ir.sahab.sonar.gerrit.integrator.service.GerritService;
import ir.sahab.sonar.gerrit.integrator.service.SonarQubeService;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SonarGerritIntegratorApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Autowired
	GerritService gerritService;

	@Autowired
	SonarQubeService sonarQubeService;

	private MockRestServiceServer sonarServer;
	private MockRestServiceServer gerritServer;

	@BeforeEach
	void setup() {
		sonarServer = MockRestServiceServer.createServer(sonarQubeService.getRestTemplate());
		gerritServer = MockRestServiceServer.createServer(gerritService.getRestTemplate());
	}

	@Test
	void testWebhook() throws URISyntaxException, IOException {
		Path sonarApiResponseFilePath = getResourceFilePath("/response.json");
		String sonarApi = "http://localhost/api/issues/search?"
				+ "pullRequest=pKey&componentKeys=project&resolved=false";
		String sonarAuthHeader = basicAuthorizationHeader("sonarToken", "");
		sonarServer.expect(requestTo(sonarApi))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", sonarAuthHeader))
				.andRespond(withSuccess(Files.readAllBytes(sonarApiResponseFilePath),
						MediaType.APPLICATION_JSON));

		String gerritAuthHeader = basicAuthorizationHeader("ci", "gerritToken");
		gerritServer.expect(requestTo("http://localhost/a/changes/1147/revisions/7/review"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", gerritAuthHeader))
				.andRespond(withSuccess());

		String webhookContent = Files.readString(getResourceFilePath("/webhook.json"));
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		RequestEntity<String> webhookRequest =
				new RequestEntity<>(webhookContent, headers, null, null, null);
		ResponseEntity<String> response = restTemplate
				.exchange("/", HttpMethod.POST, webhookRequest, String.class);

		assertTrue(response.getStatusCode().is2xxSuccessful());
		sonarServer.verify();
		gerritServer.verify();
	}

	private static String basicAuthorizationHeader(String username, String password) {
		return "Basic " + Base64.getEncoder()
				.encodeToString((username + ":" + password).getBytes());
	}

	private Path getResourceFilePath(String name) throws URISyntaxException {
		return Paths.get(Objects.requireNonNull(this.getClass().getResource(name)).toURI());
	}
}
