package tdl.datapoint.sourcecode;

import com.amazonaws.services.lambda.runtime.Context;
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

    private static final Context NO_CONTEXT = null;

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

        //TODO replace with queue client
        String queueName = "queue2";
        queueUrl = LocalSQSQueue.getQueueUrlOrCreate(queueName);
        LocalSQSQueue.purgeQueue(queueName);
        environmentVariables.set(SQSMessageQueue.ENV_SQS_QUEUE_URL, queueUrl);

        sourceCodeUploadHandler = new SourceCodeUploadHandler();
    }

    private String uploadSrcsToS3(File srcsFile, String key) {
        AmazonS3 s3Client = LocalS3Bucket.createS3Client();
        createBucketIfNotExists(s3Client, BUCKET);
        s3Client.putObject(BUCKET, key, srcsFile);
        return "{\"Records\":[{\"s3\":{\"bucket\":{\"name\":\"" + BUCKET + "\"},"
                + " \"object\":{\"key\":\"" + key + "\"}}}]}";
    }

    static class TestSrcsFile {
        Path resourcePath;

        TestSrcsFile(String name) {
            resourcePath = Paths.get("src/test/resources/", name);
        }

        File asFile() {
            return resourcePath.toFile();
        }

        List<String> getCommitMessages() throws IOException {
            Reader reader = new Reader(asFile());
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
    }

    @Test
    public void create_repo_and_uploads_commits() throws Exception {
        // Given - The participant produces SRCS files while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();
        String s3destination = String.format("%s/%s/file.srcs", challengeId, participantId);
        TestSrcsFile srcs1 = new TestSrcsFile("test.srcs");
        TestSrcsFile srcs2 = new TestSrcsFile("test.srcs");
        List<String> combinedSrcsMessages = new ArrayList<>();
        combinedSrcsMessages.addAll(srcs1.getCommitMessages());
        combinedSrcsMessages.addAll(srcs2.getCommitMessages());

        // When - Upload event happens
        sourceCodeUploadHandler.handleRequest(
                convertToMap(uploadSrcsToS3(srcs1.asFile(), s3destination)),
                NO_CONTEXT);

        // Then - Repo is created with the contents of the SRCS file
        String repoUrl1 = LocalSQSQueue.getFirstMessageBody(queueUrl);
        assertThat(repoUrl1, allOf(startsWith("file:///"),
//                containsString(challengeId),
                endsWith(participantId)));
        assertThat(getCommitMessagesFromGit(repoUrl1), equalTo(srcs1.getCommitMessages()));

        // When - Another upload event happens
        sourceCodeUploadHandler.handleRequest(
                convertToMap(uploadSrcsToS3(srcs2.asFile(), s3destination)),
                NO_CONTEXT);

        // Then - The SRCS file is appended to the repo
        String repoUrl2 = LocalSQSQueue.getFirstMessageBody(queueUrl);
        assertThat(repoUrl1, equalTo(repoUrl2));
        assertThat(getCommitMessagesFromGit(repoUrl2), equalTo(combinedSrcsMessages));
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

    private static List<String> getCommitMessagesFromGit(String actual) throws Exception {
        Git git = Git.open(new File(new URI(actual)));
        List<String> messages = new ArrayList<>();
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            messages.add(commit.getFullMessage());
        }
        Collections.reverse(messages);
        return messages;
    }

    private static Map<String, Object> convertToMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
