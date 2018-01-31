package tdl.datapoint.sourcecode.processing;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.Optional;

public class RemoteGithub {
    private final RepositoryService service;
    private final String organisation;

    public RemoteGithub(String host, int port,
                        String protocol,
                        String organisation,
                        String authToken) {
        GitHubClient client = new GitHubClient(host, port, protocol);
        client.setOAuth2Token(authToken);

        System.out.println("client.getRemainingRequests() = " + client.getRemainingRequests());
        System.out.println("client.getRequestLimit() = " + client.getRequestLimit());


        this.organisation = organisation;
        this.service = new RepositoryService(client);
    }

    public Repository createNewRepositoryIfNotExists(String challengeId, String participantId) throws GithubInteractionException {
        String repoName = challengeId + "-" + participantId;
        Optional<Repository> repository = getRepository(repoName);
        if (repository.isPresent()) {
            return repository.get();
        } else {
            return createNewRepo(repoName);
        }
    }

    private Repository createNewRepo(String repoName) throws GithubInteractionException {
        Repository newRepo = new Repository();
        newRepo.setName(repoName);
        User owner = new User();
        owner.setName(organisation);
        newRepo.setOwner(owner);

        try {
            return service.createRepository(organisation, newRepo);
        } catch (IOException e) {
            throw new GithubInteractionException("Failed to create repo", e);
        }
    }

    private Optional<Repository> getRepository(String repoName) throws GithubInteractionException {
        try {
            return Optional.of(service.getRepository(organisation, repoName));
        } catch (RequestException e) {
            if (e.getStatus() == 404) {
                return Optional.empty();
            } else {
                throw new GithubInteractionException("Failed to create repo", e);
            }
        } catch (IOException e) {
            throw new GithubInteractionException("Failed to create repo", e);
        }
    }
}
