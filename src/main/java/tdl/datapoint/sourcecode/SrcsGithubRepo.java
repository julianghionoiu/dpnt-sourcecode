package tdl.datapoint.sourcecode;

import java.io.IOException;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

public final class SrcsGithubRepo {

    private final String s3Key;

    private final String username;

    private final GitHubClient client;

    public SrcsGithubRepo(String s3Key) {
        this.s3Key = s3Key;
        this.username = getDefaultUser();
        client = new GitHubClient();
    }

    public SrcsGithubRepo(String s3Key, GitHubClient client) {
        this.s3Key = s3Key;
        this.client = client;
        this.username = getDefaultUser();
    }

    public boolean isGithubRepoExistsForKey() {
        RepositoryService service = new RepositoryService(client);
        try {
            Repository repository = service.getRepository(username, s3Key);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public String getRepositoryName() {
        return "";
    }

    public String getDefaultUser() {
        return System.getenv("GITHUB_DEFAULT_USER");
    }

    public String getUri() {
        return "";
    }
}
