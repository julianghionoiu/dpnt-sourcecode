package tdl.datapoint.sourcecode.processing;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalGitClient {
    private final UsernamePasswordCredentialsProvider credentialsProvider;

    public LocalGitClient(String authToken) {
        credentialsProvider =
                new UsernamePasswordCredentialsProvider(authToken, "");
    }

    public Git cloneToTemp(String cloneUrl) throws IOException, GitAPIException {
        Path directory = Files.createTempDirectory("srcs_repo_");
        return Git.cloneRepository()
                .setURI(cloneUrl)
                .setCredentialsProvider(credentialsProvider)
                .setDirectory(directory.toFile())
                .call();
    }

    public void pushToRemote(Git localRepo) throws GitAPIException {
        localRepo.push()
                .setCredentialsProvider(credentialsProvider)
                .call();
        localRepo.push()
                .setCredentialsProvider(credentialsProvider)
                .setPushTags()
                .call();
    }
}
