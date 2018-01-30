package tdl.datapoint.sourcecode;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import tdl.datapoint.sourcecode.support.LocalGithub;
import tdl.datapoint.sourcecode.support.LocalS3Bucket;
import tdl.datapoint.sourcecode.support.LocalSQSQueue;
import tdl.datapoint.sourcecode.support.TestSrcsFile;
import tdl.participant.queue.connector.EventProcessingException;
import tdl.participant.queue.connector.QueueEventHandlers;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.SourceCodeUpdatedEvent;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class SourcecodeDatapointAcceptanceTest {
    private static final Context NO_CONTEXT = null;

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private SourceCodeUploadHandler sourceCodeUploadHandler;
    private SqsEventQueue sqsEventQueue;
    private Stack<SourceCodeUpdatedEvent> sourceCodeUpdatedEvents;

    @Before
    public void setUp() throws EventProcessingException {
        //DEBT should read this from the `config/env.local.yml`
        env(ApplicationEnv.GITHUB_REPO_OWNER, LocalGithub.GITHUB_REPO_OWNER);
        env(ApplicationEnv.GITHUB_AUTH_TOKEN, LocalGithub.GITHUB_TOKEN);
        env(ApplicationEnv.GITHUB_HOST, LocalGithub.GITHUB_HOST);
        env(ApplicationEnv.GITHUB_PORT, LocalGithub.GITHUB_PORT);
        env(ApplicationEnv.GITHUB_PROTOCOL, LocalGithub.GITHUB_PROTOCOL);

        env(ApplicationEnv.SQS_ENDPOINT, LocalSQSQueue.ELASTIC_MQ_URL);
        env(ApplicationEnv.SQS_REGION, LocalSQSQueue.ELASTIC_MQ_REGION);
        env(ApplicationEnv.SQS_ACCESS_KEY, LocalSQSQueue.ELASTIC_MQ_ACCESS_KEY);
        env(ApplicationEnv.SQS_SECRET_KEY, LocalSQSQueue.ELASTIC_MQ_SECRET_KEY);
        env(ApplicationEnv.SQS_QUEUE_URL, LocalSQSQueue.ELASTIC_MQ_QUEUE_URL);

        env(ApplicationEnv.S3_ENDPOINT, LocalS3Bucket.MINIO_URL);
        env(ApplicationEnv.S3_REGION, LocalS3Bucket.MINIO_REGION);
        env(ApplicationEnv.S3_ACCESS_KEY, LocalS3Bucket.MINIO_ACCESS_KEY);
        env(ApplicationEnv.S3_SECRET_KEY, LocalS3Bucket.MINIO_SECRET_KEY);

        sourceCodeUploadHandler = new SourceCodeUploadHandler();

        sqsEventQueue = LocalSQSQueue.createInstance();

        QueueEventHandlers queueEventHandlers = new QueueEventHandlers();
        sourceCodeUpdatedEvents = new Stack<>();
        queueEventHandlers.put(SourceCodeUpdatedEvent.class, sourceCodeUpdatedEvents::add);
        sqsEventQueue.subscribeToMessages(queueEventHandlers);
    }

    private void env(ApplicationEnv key, String value) {
        environmentVariables.set(key.name(), value);
    }

    @After
    public void tearDown() throws Exception {
        sqsEventQueue.unsubscribeFromMessages();
    }

    @Test
    public void create_repo_and_uploads_commits() throws Exception {
        // Given - The participant produces SRCS files while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();
        String s3destination = String.format("%s/%s/file.srcs", challengeId, participantId);
        TestSrcsFile srcs1 = new TestSrcsFile("test1.srcs");
        TestSrcsFile srcs2 = new TestSrcsFile("test2.srcs");

        // When - Upload event happens
        sourceCodeUploadHandler.handleRequest(
                convertToMap(LocalS3Bucket.putObject(srcs1.asFile(), s3destination)),
                NO_CONTEXT);

        // Then - Repo is created with the contents of the SRCS file
        waitForQueueToReceiveEvents();
        SourceCodeUpdatedEvent queueEvent1 = sourceCodeUpdatedEvents.pop();
        String repoUrl1 = queueEvent1.getSourceCodeLink();
        assertThat(repoUrl1, allOf(startsWith("file:///"),
                containsString(challengeId),
                endsWith(participantId)));
        assertThat(LocalGithub.getCommitMessages(repoUrl1), equalTo(srcs1.getCommitMessages()));
        assertThat(LocalGithub.getTags(repoUrl1), equalTo(srcs1.getTags()));

        // When - Another upload event happens
        sourceCodeUploadHandler.handleRequest(
                convertToMap(LocalS3Bucket.putObject(srcs2.asFile(), s3destination)),
                NO_CONTEXT);

        // Then - The SRCS file is appended to the repo
        waitForQueueToReceiveEvents();
        SourceCodeUpdatedEvent queueEvent2 = sourceCodeUpdatedEvents.pop();
        String repoUrl2 = queueEvent2.getSourceCodeLink();
        assertThat(repoUrl1, equalTo(repoUrl2));
        assertThat(LocalGithub.getCommitMessages(repoUrl2), equalTo(getCombinedMessages(srcs1, srcs2)));
        assertThat(LocalGithub.getTags(repoUrl2), equalTo(getCombinedTags(srcs1, srcs2)));
    }

    //~~~~~~~~~~ Helpers ~~~~~~~~~~~~~`

    private static void waitForQueueToReceiveEvents() throws InterruptedException {
        Thread.sleep(500);
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    private static Map<String, Object> convertToMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private static List<String> getCombinedMessages(TestSrcsFile srcs1, TestSrcsFile srcs2) throws IOException {
        List<String> combinedList = new ArrayList<>();
        combinedList.addAll(srcs1.getCommitMessages());
        combinedList.addAll(srcs2.getCommitMessages());
        return combinedList;
    }

    private static List<String> getCombinedTags(TestSrcsFile srcs1, TestSrcsFile srcs2) throws IOException {
        List<String> combinedList = new ArrayList<>();
        combinedList.addAll(srcs1.getTags());
        combinedList.addAll(srcs2.getTags());
        return combinedList;
    }
}
