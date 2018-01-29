package tdl.datapoint.sourcecode;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class SourceCodeUploadHandler implements RequestHandler<Map<String, Object>, String> {
    private static final Logger LOG = Logger.getLogger(SourceCodeUploadHandler.class.getName());
    private AmazonS3 s3Client;

    SourceCodeUploadHandler() {
        s3Client = createDefaultS3Client();

        // S3
        // Github repo
        // SQS queue
    }

    @Override
    public String handleRequest(Map<String, Object> s3EventMap, Context context) {
        try {
            S3BucketEvent event = S3BucketEvent.from(s3EventMap);

            uploadCommitToRepo(event);
            return "OK";
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    //Debt should be private and we should construct the Map as AWS does
    private void uploadCommitToRepo(S3BucketEvent event) throws Exception {
        S3Object s3Object = s3Client.getObject(event.getBucket(), event.getKey());

        String repoName = SrcsGithubRepo.parseS3KeyToRepositoryName(event.getKey());

        GitHubClient githubClient = SrcsGithubRepo.createGithubClient();
        SrcsGithubRepo repo1 = new SrcsGithubRepo(repoName, githubClient);
        repo1.createNewRepositoryIfNotExists();
        Git git = getGitRepo(repo1);

        S3SrcsToGitExporter exporter = new S3SrcsToGitExporter(s3Object, git);
        exporter.export();

        pushRemote(git);

        sendGithubUrlToQueue(repo1.getUri());
    }

    private void sendGithubUrlToQueue(String url) {
        SQSMessageQueue queue = new SQSMessageQueue();
        queue.send(url);
    }

    private void pushRemote(Git git) throws GitAPIException {
        git.push()
                .setCredentialsProvider(SrcsGithubRepo.getCredentialsProvider())
                .call();
    }

    private Git getGitRepo(SrcsGithubRepo repo) throws Exception {
        Path directory = Files.createTempDirectory("tmp");
        return Git.cloneRepository()
                .setURI(repo.getUri())
                .setCredentialsProvider(SrcsGithubRepo.getCredentialsProvider())
                .setDirectory(directory.toFile())
                .call();
    }

    private AmazonS3 createDefaultS3Client() {
        return S3SrcsToGitExporter.createDefaultS3Client();
    }
}
