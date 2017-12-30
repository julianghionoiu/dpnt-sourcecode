package tdl.datapoint.sourcecode;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import java.util.logging.Level;
import org.eclipse.egit.github.core.client.GitHubClient;

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
        String uri = repo.getUri();
        S3SrcsToGitExporter exporter = new S3SrcsToGitExporter(event.getBucket(), event.getKey(), uri, createDefaultS3Client());
        exporter.export();
        sendGithubUrlToQueue(uri);
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

    public GitHubClient createDefaultGithubClient() {
        return SrcsGithubRepo.createDefaultGithubClient();
    }

    public AmazonSQS createDefaultSQSClient() {
        return SQSMessageQueue.createDefaultSqsClient();
    }

    public AmazonS3 createDefaultS3Client() {
        return S3SrcsToGitExporter.createDefaultS3Client();
    }
}
