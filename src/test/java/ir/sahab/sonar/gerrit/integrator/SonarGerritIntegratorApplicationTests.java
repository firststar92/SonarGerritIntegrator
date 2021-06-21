package ir.sahab.sonar.gerrit.integrator;

import ir.sahab.sonar.gerrit.integrator.service.GerritService;
import ir.sahab.sonar.gerrit.integrator.service.SonarQubeService;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
		Path path = Paths.get(this.getClass().getResource("/response.json").toURI());

		String authHeader = "Basic " + Base64.getEncoder().encodeToString(
				("sonarToken" + ':').getBytes());
		sonarServer.expect(requestTo("http://localhost/api/issues/search?pullRequest=pKey&componentKeys=ir.sahab:project"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", authHeader))
				.andRespond(withSuccess(Files.readAllBytes(path), MediaType.APPLICATION_JSON));

		authHeader = "Basic " + Base64.getEncoder().encodeToString(
				("ci" + ':' + "gerritToken").getBytes());
		gerritServer.expect(requestTo("http://localhost/a/changes/1147/revisions/7/review"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", authHeader))
				.andExpect(content().string(new Matcher<String>() {
					@Override
					public boolean matches(Object o) {
						System.out.println(o);
						return true;
					}

					@Override
					public void describeMismatch(Object o, Description description) {

					}

					@Override
					public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {

					}

					@Override
					public void describeTo(Description description) {

					}
				}))
				.andRespond(withSuccess());

		path = Paths.get(this.getClass().getResource("/webhook.json").toURI());
		String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		RequestEntity<String> e = new RequestEntity<>(data, headers, null, null, null);
		ResponseEntity<String> response = restTemplate.exchange("/", HttpMethod.POST, e, String.class);

		assertTrue(response.getStatusCode().is2xxSuccessful());
		sonarServer.verify();
		gerritServer.verify();
	}
}
