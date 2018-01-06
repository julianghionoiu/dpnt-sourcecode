package tdl.datapoint.sourcecode;

import java.io.IOException;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class SrcsGithubRepo {

    private final String repoName;

    private final String username;

    private final GitHubClient client;

    private final RepositoryService service;

    private Repository repository;
    
    private String cloneUrl;

    public SrcsGithubRepo(String repoName) {
        this.repoName = repoName;
        this.client = createDefaultGithubClient();
        this.username = getUsername();
        this.service = new RepositoryService(client);
    }

    public SrcsGithubRepo(String repoName, GitHubClient client) {
        this.repoName = repoName;
        this.client = client;
        this.username = getUsername();
        this.service = new RepositoryService(client);
    }

    public void createNewRepositoryIfNotExists() {
        try {
            createNewRepository();
        } catch (IOException e) {
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

    public static String getUsername() {
        return System.getenv("GITHUB_USERNAME");
    }

    public static GitHubClient createDefaultGithubClient() {
        GitHubClient defaultClient = new GitHubClient();
        defaultClient.setOAuth2Token(getToken());
        return defaultClient;
    }

    public static CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(getToken(), "");
    }

    public static String getToken() {
        return System.getenv("GITHUB_TOKEN");
    }

    public String getUri() {
        return repository.getCloneUrl();
    }

    public static String parseS3KeyToRepositoryName(String s3Key) {
        return s3Key.split("/")[1];
    }
}
