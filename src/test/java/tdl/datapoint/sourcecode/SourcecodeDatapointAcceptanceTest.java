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
import org.yaml.snakeyaml.Yaml;
import tdl.datapoint.sourcecode.support.*;
import tdl.participant.queue.connector.EventProcessingException;
import tdl.participant.queue.connector.QueueEventHandlers;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.SourceCodeUpdatedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
    private LocalS3Bucket localS3Bucket;
    private Stack<SourceCodeUpdatedEvent> sourceCodeUpdatedEvents;

    @Before
    public void setUp() throws EventProcessingException, IOException {
        environmentVariables.set("AWS_ACCESS_KEY_ID","local_test_access_key");
        environmentVariables.set("AWS_SECRET_KEY","local_test_secret_key");
        setEnvFrom(environmentVariables, Paths.get("config", "env.local.yml"));

        localS3Bucket = LocalS3Bucket.createInstance(
                getEnv(ApplicationEnv.S3_ENDPOINT),
                getEnv(ApplicationEnv.S3_REGION));

        sqsEventQueue = LocalSQSQueue.createInstance(
                getEnv(ApplicationEnv.SQS_ENDPOINT),
                getEnv(ApplicationEnv.SQS_REGION),
                getEnv(ApplicationEnv.SQS_QUEUE_URL));

        sourceCodeUploadHandler = new SourceCodeUploadHandler();

        QueueEventHandlers queueEventHandlers = new QueueEventHandlers();
        sourceCodeUpdatedEvents = new Stack<>();
        queueEventHandlers.on(SourceCodeUpdatedEvent.class, sourceCodeUpdatedEvents::add);
        sqsEventQueue.subscribeToMessages(queueEventHandlers);
    }

    private static String getEnv(ApplicationEnv key) {
        String env = System.getenv(key.name());
        if (env == null || env.trim().isEmpty() || "null".equals(env)) {
            throw new RuntimeException("[Startup] Environment variable " + key + " not set");
        }
        return env;
    }

    private static void setEnvFrom(EnvironmentVariables environmentVariables, Path path) throws IOException {
        String yamlString = Files.lines(path).collect(Collectors.joining("\n"));

        Yaml yaml = new Yaml();
        Map<String, String> values = yaml.load(yamlString);

        values.forEach(environmentVariables::set);
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
                convertToMap(localS3Bucket.putObject(srcs1.asFile(), s3destination)),
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
                convertToMap(localS3Bucket.putObject(srcs2.asFile(), s3destination)),
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
