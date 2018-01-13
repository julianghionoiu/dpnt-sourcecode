package tdl.datapoint.sourcecode;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Handler implements RequestHandler<Map<String, Object>, Response> {

    //private static final Logger LOG = Logger.getLogger(Handler.class);
    @Override
    public Response handleRequest(Map<String, Object> input, Context context) {
        try {
            S3BucketEvent event = new S3BucketEvent(input);
            uploadCommitToRepo(event);
            return new Response("ok");
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            return new Response("error: " + ex.getMessage());
        }
    }

    public void uploadCommitToRepo(S3BucketEvent event) throws Exception {
        SrcsGithubRepo repo = createRepository(event.getKey());
        S3Object s3Object = getS3Object(event);
        Git git = getGitRepo(repo);
        S3SrcsToGitExporter exporter = new S3SrcsToGitExporter(s3Object, git);
        exporter.export();
        pushRemote(git);
        sendGithubUrlToQueue(repo.getUri());
    }

    private SrcsGithubRepo createRepository(String s3Key) throws Exception {
        String repoName = SrcsGithubRepo.parseS3KeyToRepositoryName(s3Key);
        SrcsGithubRepo repo = new SrcsGithubRepo(repoName, createDefaultGithubClient());
        repo.createNewRepositoryIfNotExists();
        return repo;
    }

    private void sendGithubUrlToQueue(String url) {
        SQSMessageQueue queue = new SQSMessageQueue(createDefaultSQSClient());
        queue.send(url);
    }

    private void pushRemote(Git git) throws GitAPIException {
        git.push()
                .setCredentialsProvider(SrcsGithubRepo.getCredentialsProvider())
                .call();
    }

    public Git getGitRepo(SrcsGithubRepo repo) throws Exception {
        Path directory = Files.createTempDirectory("tmp");
        return Git.cloneRepository()
                .setURI(repo.getUri())
                .setCredentialsProvider(SrcsGithubRepo.getCredentialsProvider())
                .setDirectory(directory.toFile())
                .call();
    }

    public S3Object getS3Object(S3BucketEvent event) {
        AmazonS3 s3Client = createDefaultS3Client();
        return s3Client.getObject(event.getBucket(), event.getKey());
    }

    public GitHubClient createDefaultGithubClient() {
        return SrcsGithubRepo.createGithubClient();
    }

    public AmazonSQS createDefaultSQSClient() {
        return SQSMessageQueue.createDefaultSqsClient();
    }

    public AmazonS3 createDefaultS3Client() {
        return S3SrcsToGitExporter.createDefaultS3Client();
    }
}
