package tdl.datapoint.sourcecode;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;

public class RemoteGithub {

    public static final String ENV_GITHUB_REPO_OWNER = "GITHUB_USERNAME";
    public static final String ENV_GITHUB_HOST = "GITHUB_HOST";
    public static final String ENV_GITHUB_PORT = "GITHUB_PORT";
    public static final String ENV_GITHUB_PROTOCOL = "GITHUB_PROTOCOL";
    public static final String ENV_GITHUB_AUTH_TOKEN = "GITHUB_TOKEN";

    private final String repoOwner;

    private final RepositoryService service;

    RemoteGithub(String host, int port, String protocol, String authToken,
                 String repoOwner) {
        GitHubClient client = new GitHubClient(host, port, protocol);
        client.setOAuth2Token(authToken);

        this.repoOwner = repoOwner;
        this.service = new RepositoryService(client);
    }

    public Repository createNewRepositoryIfNotExists(String challengeId, String participantId) throws GithubInteractionException {
        String repoName = challengeId + "-" + participantId;

        Repository newRepo = new Repository();
        newRepo.setName(repoName);

        User owner = new User();
        owner.setName(repoOwner);
        newRepo.setOwner(owner);

        try {
            return service.createRepository(newRepo);
        } catch (IOException e) {
            throw new GithubInteractionException("Failed to create repo", e);
        }
    }

}
