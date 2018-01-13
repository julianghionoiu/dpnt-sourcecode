package tdl.datapoint.sourcecode;

import java.io.IOException;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.junit.Test;

public class GithubClientTest {

    @Test
    public void run() throws IOException {
        GitHubClient client = new GitHubClient("localhost", 9556, "http");
        client.setCredentials("user", "123");
        RepositoryService service = new RepositoryService(client);
        Repository repo = service.getRepository("user", "repo");
        System.out.println(repo.getCloneUrl());
//        repo.setName("repo");
//        service.createRepository(repo);
    }
}
