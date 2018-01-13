package tdl.datapoint.sourcecode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.egit.github.core.client.GitHubClient;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class SrcsGithubRepoTest {

    private static final String REPO_DIR = "/tmp/repo";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    public GitHubClient client;

    @Before
    public void setUp() {
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_USERNAME, "user1");
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_HOST, "localhost");
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_PORT, "9556");
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_PROTOCOL, "http");
    }

    @Test
    public void doesGithubRepoExistForKeyShouldReturnTrue() throws GitAPIException, IOException {
        File directory = new File(REPO_DIR + "/repo1");
        FileUtils.deleteDirectory(directory); 
        Git.init().setDirectory(directory).call();
        SrcsGithubRepo repo = new SrcsGithubRepo("repo1");
        assertTrue(repo.doesGithubRepoExist());
    }

    @Test
    public void doesGithubRepoExistForKeyShouldReturnFalse() {
        FileUtils.deleteQuietly(new File(REPO_DIR + "/repo2"));
        SrcsGithubRepo repo = new SrcsGithubRepo("repo2 ");
        assertFalse(repo.doesGithubRepoExist());
    }

    @Test
    public void parseS3KeyToRepositoryName() {
        assertEquals("username", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/username/file.srcs"));
        assertEquals("user1", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/user1/file.srcs"));
    }

    @Test
    public void createNewRepository() throws IOException {
        SrcsGithubRepo repo = new SrcsGithubRepo("repository1");
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
//        environmentVariables.set("GITHUB_USERNAME", "dpnttest");
//        environmentVariables.set("GITHUB_TOKEN", "");
//        SrcsGithubRepo repo = new SrcsGithubRepo("test3");
//        Repository newRepo = repo.createNewRepository();
//        assertNotNull(newRepo);
    }
}
