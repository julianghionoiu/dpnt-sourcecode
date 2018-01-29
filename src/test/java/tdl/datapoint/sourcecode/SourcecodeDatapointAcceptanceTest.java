package tdl.datapoint.sourcecode;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import tdl.datapoint.sourcecode.support.LocalS3Bucket;
import tdl.datapoint.sourcecode.support.LocalSQSQueue;
import tdl.record.sourcecode.snapshot.file.Header;
import tdl.record.sourcecode.snapshot.file.Reader;
import tdl.record.sourcecode.snapshot.file.Segment;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class SourcecodeDatapointAcceptanceTest {

    private static final String BUCKET = "localbucket";

    private static final String GITHUB_USERNAME = "dpnttest";

    private static final String GITHUB_TOKEN = "test";

    private static final String GITHUB_HOST = "localhost";

    private static final String GITHUB_PORT = "9556";

    private static final String GITHUB_PROTOCOL = "http";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String queueUrl;
    private SourceCodeUploadHandler sourceCodeUploadHandler;

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

        //
        String queueName = "queue2";
        queueUrl = LocalSQSQueue.getQueueUrlOrCreate(queueName);
        LocalSQSQueue.purgeQueue(queueName);
        environmentVariables.set(SQSMessageQueue.ENV_SQS_QUEUE_URL, queueUrl);

        sourceCodeUploadHandler = new SourceCodeUploadHandler();
    }

    private String uploadSrcsToS3(String srcsPath, String key) {
        AmazonS3 s3Client = LocalS3Bucket.createS3Client();
        Path path = Paths.get(srcsPath);
        createBucketIfNotExists(s3Client, BUCKET);
        s3Client.putObject(BUCKET, key, path.toFile());
        return "{\"Records\":[{\"s3\":{\"bucket\":{\"name\":\"" + BUCKET + "\"},"
                + " \"object\":{\"key\":\"" + key + "\"}}}]}";
    }

    @Test
    public void create_repo_and_uploads_commits() throws Exception {
        String challengeId = generateId();
        String participantId = generateId();

        String srcsPath = "src/test/resources/test.srcs";
        String key = String.format("%s/%s/file.srcs", challengeId, participantId);
        String s3UploadEventJson = uploadSrcsToS3(srcsPath, key);

        // Invoke the handler as Lambda would
        sourceCodeUploadHandler.handleRequest(convertToMap(s3UploadEventJson), null);

        String publishedRepoUrl = LocalSQSQueue.getFirstMessageBody(queueUrl);
        assertThat(publishedRepoUrl, allOf(startsWith("file:///"),
//                containsString(challengeId),
                endsWith(participantId)));
        assertThat(getCommitMessagesFromGit(publishedRepoUrl),
                equalTo(getCommitMessagesFromSrcs(Paths.get(srcsPath))));


    }

    //~~~~~~~~~~ Helpers ~~~~~~~~~~~~~`

    private static String generateId() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    @SuppressWarnings("deprecation")
    private void createBucketIfNotExists(AmazonS3 client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

    private static List<String> getCommitMessagesFromSrcs(Path path) throws IOException {
        Reader reader = new Reader(path.toFile());
        List<String> messages = new ArrayList<>();
        while (reader.hasNext()) {
            Header header = reader.getFileHeader();
            Segment segment = reader.nextSegment();
            Date timestamp = new Date((header.getTimestamp() + segment.getTimestampSec()) * 1000L);
            String message = timestamp.toString();
            messages.add(message);
        }
        return messages;
    }

    private static List<String> getCommitMessagesFromGit(String actual) throws Exception {
        Git git = Git.open(new File(new URI(actual)));
        List<String> messages = new ArrayList<>();
        Iterable<RevCommit> commits = git.log().call();
        Iterator<RevCommit> it = commits.iterator();
        while (it.hasNext()) {
            RevCommit commit = it.next();
            messages.add(commit.getFullMessage());
        }
        Collections.reverse(messages);
        return messages;
    }

    private static Map<String, Object> convertToMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return map;
    }
}
