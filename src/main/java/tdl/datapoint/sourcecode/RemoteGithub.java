package tdl.datapoint.sourcecode;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;

public class RemoteGithub {

    public static final String ENV_GITHUB_USERNAME = "GITHUB_USERNAME";
    public static final String ENV_GITHUB_HOST = "GITHUB_HOST";
    public static final String ENV_GITHUB_PORT = "GITHUB_PORT";
    public static final String ENV_GITHUB_PROTOCOL = "GITHUB_PROTOCOL";
    public static final String ENV_GITHUB_TOKEN = "GITHUB_TOKEN";

    private final String repoOwner;

    private final RepositoryService service;

    private Repository repository;

    RemoteGithub(String repoOwner, GitHubClient client) {
        this.repoOwner = repoOwner;
        this.service = new RepositoryService(client);
    }

    public Repository createNewRepositoryIfNotExists(String repoName) throws GithubInteractionException {
        try {
            return createNewRepository(repoName);
        } catch (IOException e) {
            throw new GithubInteractionException("Failed to create repo", e);
        }
    }

    private boolean doesGithubRepoExist(String repoName) {
        try {
            repository = service.getRepository(repoOwner, repoName);
            return repository.getId() > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    private Repository createNewRepository(String repoName) throws IOException {
        Repository newRepo = new Repository();
        newRepo.setName(repoName);

        User owner = new User();
        owner.setName(repoOwner);
        newRepo.setOwner(owner);
        newRepo = service.createRepository(newRepo);

        doesGithubRepoExist(repoName);

        return newRepo;
    }

    public static GitHubClient createGithubClient() {
        String githubHost = System.getenv(ENV_GITHUB_HOST);
        String githubPort = System.getenv(ENV_GITHUB_PORT);
        String githubProtocol = System.getenv(ENV_GITHUB_PROTOCOL);
        String authToken = System.getenv(ENV_GITHUB_TOKEN);

        GitHubClient githubClient = new GitHubClient(githubHost, Integer.parseInt(githubPort), githubProtocol);
        if (authToken != null) {
            githubClient.setOAuth2Token(authToken);
        }

        return githubClient;
    }

    public String getUri() {
        return repository.getCloneUrl();
    }

    public static String parseS3KeyToRepositoryName(String s3Key) {
        return s3Key.split("/")[1];
    }
}
