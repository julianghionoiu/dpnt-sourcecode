package tdl.datapoint.sourcecode;

import java.io.IOException;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;

public final class SrcsGithubRepo {

    private final String repoName;

    private final String username;

    private final GitHubClient client;

    private final RepositoryService service;

    private Repository repository;

    public SrcsGithubRepo(String repoName) {
        this.repoName = repoName;
        this.client = createDefaultGithubClient();
        this.username = getDefaultUsername();
        this.service = new RepositoryService(client);
    }

    public SrcsGithubRepo(String repoName, GitHubClient client) {
        this.repoName = repoName;
        this.client = client;
        this.username = getDefaultUsername();
        this.service = new RepositoryService(client);
    }

    public void createNewRepositoryIfNotExists() throws IOException {
        try {
            createNewRepository();
        } catch (RequestException e) {
            //DO NOTHING
        }
    }

    public boolean doesGithubRepoExist() {
        try {
            repository = service.getRepository(username, repoName);
            return repository.getId() > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    public Repository createNewRepository() throws IOException, RequestException {
        repository = new Repository();
        repository.setName(getRepoName());
        User owner = new User();
        owner.setName(username);
        repository.setOwner(owner);
        service.createRepository(repository);
        return repository;
    }

    public String getRepoName() {
        return repoName;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getDefaultUsername() {
        return System.getenv("GITHUB_DEFAULT_USER");
    }

    public static GitHubClient createDefaultGithubClient() {
        GitHubClient defaultClient = new GitHubClient();
        defaultClient.setOAuth2Token(getDefaultGithubToken());
        return defaultClient;
    }

    public static String getDefaultGithubToken() {
        return System.getenv("GITHUB_TOKEN");
    }

    public String getUri() {
        return "https://github.com/" + username + "/" + getRepoName();
    }

    public static String parseS3KeyToRepositoryName(String s3Key) {
        return s3Key.split("/")[1];
    }
}
