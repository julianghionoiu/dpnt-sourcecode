package tdl.datapoint.sourcecode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.egit.github.core.client.GitHubClient;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static Path getRepoDirPath() throws IOException {
        String path = FileUtils.readFileToString(Paths.get("tmp/github-dir.txt").toFile(), Charset.defaultCharset()).trim();
        return Paths.get(path);
    }

    @Test
    public void doesGithubRepoExistForKeyShouldReturnTrue() throws GitAPIException, IOException {
        File directory = getRepoDirPath().resolve("repo1").toFile();
        FileUtils.deleteDirectory(directory);
        Git.init().setDirectory(directory).call();
        
        SrcsGithubRepo repo = new SrcsGithubRepo("repo1");
        assertTrue(repo.doesGithubRepoExist());
        assertTrue(repo.getUri().startsWith("file:///tmp"));
        assertTrue(repo.getUri().contains("repo1"));
    }

    @Test
    public void doesGithubRepoExistForKeyShouldReturnFalse() throws IOException {
        SrcsGithubRepo repo = new SrcsGithubRepo("repo2 ");
        assertFalse(repo.doesGithubRepoExist());
    }

    @Test
    public void parseS3KeyToRepositoryName() {
        assertEquals("username", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/username/file.srcs"));
        assertEquals("user1", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/user1/file.srcs"));
    }

    @Test
    public void createNewRepository() throws IOException, GitAPIException {
        File directory = getRepoDirPath().resolve("repository1").toFile();
        FileUtils.deleteDirectory(directory);
        
        SrcsGithubRepo repo1 = new SrcsGithubRepo("repository1");
        assertFalse(repo1.doesGithubRepoExist());
        
        repo1.createNewRepository();
        
        assertTrue(repo1.doesGithubRepoExist());
        assertTrue(repo1.getUri().startsWith("file:///tmp"));
        assertTrue(repo1.getUri().contains("repository1"));
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
