package tdl.datapoint.sourcecode;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
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

    private static final String GIT_URI = "git://localhost:1234/";

    private static final String KEY = "challenge/test3/file.srcs";

    private static final String GITHUB_USERNAME = "dpnttest";

    private static final String GITHUB_TOKEN = "test";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private AmazonSQS sqsClient;

    private AmazonS3 s3Client;

    private String queueUrl;

    @Before
    public void setUp() {
        environmentVariables.set("GITHUB_USERNAME", GITHUB_USERNAME);
        environmentVariables.set("GITHUB_TOKEN", GITHUB_TOKEN);
    }

    private S3BucketEvent createEvent() {
        S3BucketEvent event = mock(S3BucketEvent.class);
        when(event.getBucket())
                .thenReturn(BUCKET);
        when(event.getKey())
                .thenReturn(KEY);
        return event;
    }

    private Handler mockHandler() throws IOException, GitAPIException, Exception {
        Handler handler = spy(Handler.class);

        when(handler.createDefaultGithubClient())
                .thenReturn(ServiceMock.createGithubClient());

        Git git = Git.cloneRepository()
                .setURI(GIT_URI)
                .setDirectory(folder.newFolder())
                .call();
        doReturn(git)
                .when(handler)
                .getGitRepo(any());

        s3Client = ServiceMock.createS3Client();
        when(handler.createDefaultS3Client())
                .thenReturn(s3Client);
        sqsClient = ServiceMock.createSQSClient();
        when(handler.createDefaultSQSClient())
                .thenReturn(sqsClient);

        Path path = Paths.get("src/test/resources/test.srcs");
        s3Client.putObject(BUCKET, KEY, path.toFile());
        createBucketIfNotExists(s3Client, "localbucket");

        String queueName = "queue2";
        GetQueueUrlResult queueUrlResult;
        try {
            queueUrlResult = sqsClient.getQueueUrl(queueName);
        } catch (QueueDoesNotExistException e) {
            sqsClient.createQueue(queueName);
            queueUrlResult = sqsClient.getQueueUrl(queueName);
        }
        queueUrl = queueUrlResult.getQueueUrl();
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest(queueUrl);
        sqsClient.purgeQueue(purgeQueueRequest);

        environmentVariables.set("SQS_QUEUE_URL", queueUrl);
        return handler;
    }

    private void mockNewRepoGithubApiResponse() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(400))
        );
        String result = readResourceFile("create_new_repository_result.json");
        stubFor(post(urlEqualTo("/api/v3/user/repos"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(result))
        );
    }

    @Test
    public void create_repo_and_uploads_commits() throws Exception {
        /**
         * Setup: - an existing S3 bucket (Minio) - no initial repo - an SQS
         * Event queue (ElasticMq)
         */

        /**
         * Input:
         *
         * Generate a new, unique username Upload of a SRCS file to the bucket.
         * The key will be something like `challenge/username/file.srcs`
         * Simulate the triggering of the lambda via an S3 event. (the input
         * should be the KEY of the newly updated file)
         */
        /**
         * Notes on the implementation: - the lambda receives an event from S3,
         * for a key like "challenge/username/file.srcs" - based on the key of
         * file we construct a Github URL like:
         * https://github.com/Challenge/username Example:
         * https://github.com/TST-challenge/username - if the repo does not
         * exist, we create the new repo - we clone the repo locally and we add
         * the commits - we push the commits - we push the URL as an event to
         * the SQS Queue
         */
        Handler handler = mockHandler();
        S3BucketEvent event = createEvent();
        mockNewRepoGithubApiResponse();
        handler.uploadCommitToRepo(event);

        /**
         * Output (the assertions):
         *
         * An event should be publish with a REPO url on the SQS Event Queue.
         * (check ElasticMq) We clone the REPO, it should contain the expected
         * commits
         */
        ReceiveMessageResult queueMessageRcsvResult = sqsClient.receiveMessage(queueUrl);
        assertEquals("https://test@github.com/dpnttest/test3", queueMessageRcsvResult.getMessages().get(0).getBody());
    }

    private void mockAlreadyExistsRepoGithubApiResponse() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(400))
        );
        stubFor(post(urlEqualTo("/api/v3/user/repos"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(422)) //already exists
        );
    }

    @Test
    public void push_commits_to_existing_repo() throws Exception {
        /**
         * Same as the previous test, the only difference is that the target
         * repo already exists: https://github.com/Challenge/username
         */
        Handler handler = mockHandler();
        S3BucketEvent event = createEvent();
        mockAlreadyExistsRepoGithubApiResponse();
        handler.uploadCommitToRepo(event);

        ReceiveMessageResult queueMessageRcsvResult = sqsClient.receiveMessage(queueUrl);
        assertEquals("https://test@github.com/dpnttest/test3", queueMessageRcsvResult.getMessages().get(0).getBody());
    }

    private String readResourceFile(String filename) {
        Path path = Paths.get("./src/test/resources/tdl/datapoint/sourcecode/" + filename);
        try {
            return FileUtils.readFileToString(path.toFile(), Charset.defaultCharset());
        } catch (IOException ex) {
            return "";
        }
    }

    @SuppressWarnings("deprecation")
    private void createBucketIfNotExists(AmazonS3 client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

}
