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

    public static final String ENV_GITHUB_USERNAME = "GITHUB_USERNAME";
    public static final String ENV_GITHUB_HOST = "GITHUB_HOST";
    public static final String ENV_GITHUB_PORT = "GITHUB_PORT";
    public static final String ENV_GITHUB_PROTOCOL = "GITHUB_PROTOCOL";
    public static final String ENV_GITHUB_TOKEN = "GITHUB_TOKEN";
    
    private final String repoName;

    private final String username;

    private final RepositoryService service;

    private Repository repository;

    SrcsGithubRepo(String repoName, GitHubClient client) {
        this.repoName = repoName;
        this.username = getUsername();
        this.service = new RepositoryService(client);
    }

    public void createNewRepositoryIfNotExists() throws GithubInteractionException {
        try {
            createNewRepository();
        } catch (IOException e) {
            throw new GithubInteractionException("Failed to create repo", e);
        }
    }

    private boolean doesGithubRepoExist() {
        try {
            repository = service.getRepository(username, repoName);
            return repository.getId() > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    private void createNewRepository() throws IOException {
        Repository newRepo = new Repository();
        newRepo.setName(getRepoName());
        User owner = new User();
        owner.setName(username);
        newRepo.setOwner(owner);
        service.createRepository(newRepo);
        
        doesGithubRepoExist();
    }

    private String getRepoName() {
        return repoName;
    }

    private static String getUsername() {
        return System.getenv(ENV_GITHUB_USERNAME);
    }

    public static GitHubClient createGithubClient() {
        String githubHost = System.getenv(ENV_GITHUB_HOST);
        String githubPort = System.getenv(ENV_GITHUB_PORT);
        String githubProtocol = System.getenv(ENV_GITHUB_PROTOCOL);
        GitHubClient defaultClient;
        if (githubHost != null && githubPort != null && githubProtocol != null) {
            defaultClient = new GitHubClient(githubHost, Integer.parseInt(githubPort), githubProtocol);
        } else {
            defaultClient = new GitHubClient();
            defaultClient.setOAuth2Token(getToken());
        }
        return defaultClient;
    }

    public static CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(getToken(), "");
    }

    private static String getToken() {
        return System.getenv(ENV_GITHUB_TOKEN);
    }

    public String getUri() {
        return repository.getCloneUrl();
    }

    public static String parseS3KeyToRepositoryName(String s3Key) {
        return s3Key.split("/")[1];
    }
}
