package tdl.datapoint.sourcecode;

import java.io.IOException;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

public final class SrcsGithubRepo {

    private final String s3Key;

    private final String username;

    private final GitHubClient client;

    private final RepositoryService service;

    private Repository repository;

    public SrcsGithubRepo(String s3Key) {
        this.s3Key = s3Key;
        this.client = new GitHubClient();
        this.username = getDefaultUser();
        this.service = new RepositoryService(client);
    }

    public SrcsGithubRepo(String s3Key, GitHubClient client) {
        this.s3Key = s3Key;
        this.client = client;
        this.username = getDefaultUser();
        this.service = new RepositoryService(client);
    }

    public boolean doesGithubRepoExist() {
        try {
            repository = service.getRepository(username, s3Key);
            return repository.getId() > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    public Repository createNewRepository() throws IOException {
        repository = new Repository();
        repository.setName(getRepositoryName());
        User owner = new User();
        owner.setName(username);
        repository.setOwner(owner);
        service.createRepository(repository);
        return repository;
    }

    public String getRepositoryName() {
        return s3Key;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getDefaultUser() {
        return System.getenv("GITHUB_DEFAULT_USER");
    }

    public String getUri() {
        String repository = getRepositoryName();
        return "https://github.com/" + username + "/" + repository;
    }

    public static String parseS3KeyToRepositoryName(String s3Key) {
        return "";
    }
}
