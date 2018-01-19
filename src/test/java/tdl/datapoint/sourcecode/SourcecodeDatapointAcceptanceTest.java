package tdl.datapoint.sourcecode;

import com.amazonaws.services.s3.AmazonS3;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
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

        environmentVariables.set(S3SrcsToGitExporter.ENV_S3_ENDPOINT, LocalS3Bucket.MINIO_URL);
        environmentVariables.set(S3SrcsToGitExporter.ENV_S3_REGION, LocalS3Bucket.MINIO_REGION);
        environmentVariables.set(S3SrcsToGitExporter.ENV_S3_ACCESS_KEY, LocalS3Bucket.MINIO_ACCESS_KEY);
        environmentVariables.set(S3SrcsToGitExporter.ENV_S3_SECRET_KEY, LocalS3Bucket.MINIO_SECRET_KEY);
    }

    private S3BucketEvent createS3BucketEvent() {
        S3BucketEvent event = mock(S3BucketEvent.class);
        when(event.getBucket())
                .thenReturn(BUCKET);
        when(event.getKey())
                .thenReturn(KEY);
        return event;
    }

    private Handler createHandler(String srcsPath) {
        Handler handler = new Handler();

        s3Client = LocalS3Bucket.createS3Client();

        Path path = Paths.get(srcsPath);
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
        String srcsPath = "src/test/resources/test.srcs";
        Handler handler = createHandler(srcsPath);
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

        Git git = Git.open(new File(new URI(actual)));
        assertEquals(6, getCommitCount(git));
        List<String> commitMessages = getCommitMessages(git);
        assertEquals(commitMessages.get(1), "Sun Dec 24 06:43:38 WIB 2017");
    }

    @Test
    public void push_commits_to_existing_repo() throws Exception {
        /*
         * Same as the previous test, the only difference is that the target
         * repo already exists: https://github.com/Challenge/username
         */
        String srcsPath = "src/test/resources/test.srcs";
        Handler handler = createHandler(srcsPath);
        S3BucketEvent event = createS3BucketEvent();
        handler.uploadCommitToRepo(event);

        String actual = LocalSQSQueue.getFirstMessageBody(queueUrl);
        assertTrue(actual.startsWith("file:///"));
        assertTrue(actual.endsWith("test3"));

        Git git = Git.open(new File(new URI(actual)));
        assertEquals(12, getCommitCount(git)); //appended
        List<String> commitMessages = getCommitMessages(git);
        assertEquals(commitMessages.get(7), "Sun Dec 24 06:43:38 WIB 2017");
    }

    @SuppressWarnings("deprecation")
    private void createBucketIfNotExists(AmazonS3 client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

    private static List<String> getCommitMessages(Git git) throws GitAPIException {
        List<String> messages = new ArrayList<>();
        Iterable<RevCommit> commits = git.log().call();
        Iterator<RevCommit> it = commits.iterator();
        while (it.hasNext()) {
            RevCommit commit = it.next();
            messages.add(commit.getFullMessage());
        }
        return messages;
    }

    private static int getCommitCount(Git git) throws GitAPIException {
        Iterable<RevCommit> commits = git.log().call();
        int count = 0;
        Iterator it = commits.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }
}
