package tdl.datapoint.sourcecode;

import com.amazonaws.services.s3.AmazonS3;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jgit.api.errors.GitAPIException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.*;

public class SourcecodeDatapointAcceptanceTest {

    private static final String BUCKET = "localbucket";

    private static final String KEY = "challenge/test3/file.srcs";

    private static final String GITHUB_USERNAME = "dpnttest";

    private static final String GITHUB_TOKEN = "test";
    
    private static final String GITHUB_HOST = "localhost";
    
    private static final String GITHUB_PORT = "9556";
    
    private static final String GITHUB_PROTOCOL = "http";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private AmazonS3 s3Client;

    private String queueUrl;

    @Before
    public void setUp() {
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_USERNAME, GITHUB_USERNAME);
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_TOKEN, GITHUB_TOKEN);
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_HOST, GITHUB_HOST);
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_PORT, GITHUB_PORT);
        environmentVariables.set(SrcsGithubRepo.ENV_GITHUB_PROTOCOL, GITHUB_PROTOCOL);
        
        environmentVariables.set(SQSMessageQueue.ENV_SQS_ENDPOINT, LocalSQSQueue.ELASTIC_MQ_URL);
        environmentVariables.set(SQSMessageQueue.ENV_SQS_REGION, LocalSQSQueue.ELASTIC_MQ_REGION);

        //Debt Inject the AWS KEY ID and TOKEN to build the S3 client
    }

    private S3BucketEvent createS3BucketEvent() {
        S3BucketEvent event = mock(S3BucketEvent.class);
        when(event.getBucket())
                .thenReturn(BUCKET);
        when(event.getKey())
                .thenReturn(KEY);
        return event;
    }

    private Handler mockHandler() {
        //Debt DO not mock or spy the production code
        Handler handler = spy(Handler.class);

        s3Client = LocalS3Bucket.createS3Client();
        when(handler.createDefaultS3Client())
                .thenReturn(s3Client);
        

        Path path = Paths.get("src/test/resources/test.srcs");
        createBucketIfNotExists(s3Client, BUCKET);
        s3Client.putObject(BUCKET, KEY, path.toFile());

        //Debt part of setup
        String queueName = "queue2";
        queueUrl = LocalSQSQueue.getQueueUrlOrCreate(queueName);
        LocalSQSQueue.purgeQueue(queueName);
        environmentVariables.set(SQSMessageQueue.ENV_SQS_QUEUE_URL, queueUrl);
        return handler;
    }

    @Test
    public void create_repo_and_uploads_commits() throws Exception {
        /*
         * Setup: - an existing S3 bucket (Minio) - no initial repo - an SQS
         * Event queue (ElasticMq)
         */

        /*
         * Input:
         *
         * Generate a new, unique username Upload of a SRCS file to the bucket.
         * The key will be something like `challenge/username/file.srcs`
         * Simulate the triggering of the lambda via an S3 event. (the input
         * should be the KEY of the newly updated file)
         */
        /*
         * Notes on the implementation: - the lambda receives an event from S3,
         * for a key like "challenge/username/file.srcs" - based on the key of
         * file we construct a Github URL like:
         * https://github.com/Challenge/username Example:
         * https://github.com/TST-challenge/username - if the repo does not
         * exist, we create the new repo - we clone the repo locally and we add
         * the commits - we push the commits - we push the URL as an event to
         * the SQS Queue
         */

        //Debt The SRCS file should be an explicit input

        Handler handler = mockHandler();
        S3BucketEvent event = createS3BucketEvent();
        handler.uploadCommitToRepo(event);

        /*
         * Output (the assertions):
         *
         * An event should be publish with a REPO url on the SQS Event Queue.
         * (check ElasticMq) We clone the REPO, it should contain the actual
         * commits
         */
        String actual = LocalSQSQueue.getFirstMessageBody(queueUrl);
        assertTrue(actual.startsWith("file:///"));
        //Debt Assert on the entire Github link
        assertTrue(actual.endsWith("test3"));

        //Debt Assert that the contents match the SRCS file (commit messages)
    }

    @Test
    public void push_commits_to_existing_repo() throws Exception {
        /*
         * Same as the previous test, the only difference is that the target
         * repo already exists: https://github.com/Challenge/username
         */
        Handler handler = mockHandler();
        S3BucketEvent event = createS3BucketEvent();
        handler.uploadCommitToRepo(event);

        String expected = LocalSQSQueue.getFirstMessageBody(queueUrl);
        assertTrue(expected.startsWith("file:///"));
        assertTrue(expected.endsWith("test3"));
    }

    @SuppressWarnings("deprecation")
    private void createBucketIfNotExists(AmazonS3 client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

}
