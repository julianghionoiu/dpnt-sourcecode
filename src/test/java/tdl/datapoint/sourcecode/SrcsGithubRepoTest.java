package tdl.datapoint.sourcecode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.egit.github.core.client.GitHubClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.Repository;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class SrcsGithubRepoTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void doesGithubRepoExistForKeyShouldReturnTrue() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"1234\"}"))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository", client);
        assertTrue(repo.doesGithubRepoExist());
    }

    @Test
    public void doesGithubRepoExistShouldReturnFalse() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(400))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository", client);
        assertFalse(repo.doesGithubRepoExist());
    }

    @Test
    public void parseS3KeyToRepositoryName() {
        assertEquals("username", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/username/file.srcs"));
        assertEquals("user1", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/user1/file.srcs"));
    }

    @Test
    public void createNewRepository() throws IOException {
        String result = readResourceFile("create_new_repository_result.json");
        stubFor(post(urlEqualTo("/api/v3/user/repos"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(result))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository1", client);
        Repository newRepo = repo.createNewRepository();
        assertEquals("repository1", newRepo.getName());
    }

    public String readResourceFile(String filename) {
        Path path = Paths.get("./src/test/resources/tdl/datapoint/sourcecode/" + filename);
        try {
            return FileUtils.readFileToString(path.toFile(), Charset.defaultCharset());
        } catch (IOException ex) {
            return "";
        }
    }

    @Test
    public void useRealCred() throws IOException {
//        environmentVariables.set("GITHUB_DEFAULT_USER", "dpnttest");
//        environmentVariables.set("GITHUB_TOKEN", "");
//        SrcsGithubRepo repo = new SrcsGithubRepo("test3");
//        Repository newRepo = repo.createNewRepository();
//        assertNotNull(newRepo);
    }
}
