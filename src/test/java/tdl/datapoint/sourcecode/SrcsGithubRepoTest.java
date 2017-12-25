package tdl.datapoint.sourcecode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.egit.github.core.client.GitHubClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class SrcsGithubRepoTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testIsGithubRepoExistsForKeyShouldReturnTrue() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"1234\"}"))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository", client);
        assertTrue(repo.isGithubRepoExistsForKey());
    }

    @Test
    public void testIsGithubRepoExistsForKeyShouldReturnFalse() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(400))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository", client);
        assertFalse(repo.isGithubRepoExistsForKey());
    }

}
