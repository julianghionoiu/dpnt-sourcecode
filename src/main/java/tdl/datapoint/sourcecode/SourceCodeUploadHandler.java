package tdl.datapoint.sourcecode;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.jgit.api.Git;
import tdl.datapoint.sourcecode.processing.LocalGitClient;
import tdl.datapoint.sourcecode.processing.RemoteGithub;
import tdl.datapoint.sourcecode.processing.S3BucketEvent;
import tdl.datapoint.sourcecode.processing.S3SrcsToGitExporter;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.SourceCodeUpdatedEvent;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tdl.datapoint.sourcecode.ApplicationEnv.*;

public class SourceCodeUploadHandler implements RequestHandler<Map<String, Object>, String> {
    private static final Logger LOG = Logger.getLogger(SourceCodeUploadHandler.class.getName());
    private AmazonS3 s3Client;
    private RemoteGithub remoteGithubClient;
    private LocalGitClient localGitClient;
    private SqsEventQueue participantEventQueue;
    private S3SrcsToGitExporter srcsToGitExporter;
    private ObjectMapper jsonObjectMapper;


    private static String getEnv(ApplicationEnv key) {
        String env = System.getenv(key.name());
        if (env == null || env.trim().isEmpty() || "null".equals(env)) {
            throw new RuntimeException("[Startup] Environment variable " + key + " not set");
        }
        return env;
    }

    @SuppressWarnings("WeakerAccess")
    public SourceCodeUploadHandler() {
        s3Client = createS3Client(
                getEnv(S3_ENDPOINT),
                getEnv(S3_REGION));

        remoteGithubClient = new RemoteGithub(
                getEnv(GITHUB_HOST),
                Integer.parseInt(getEnv(GITHUB_PORT)),
                getEnv(GITHUB_PROTOCOL),
                getEnv(GITHUB_ORGANISATION),
                getEnv(GITHUB_AUTH_TOKEN));

        localGitClient = new LocalGitClient(
                getEnv(GITHUB_AUTH_TOKEN));

        srcsToGitExporter = new S3SrcsToGitExporter();

        AmazonSQS client = createSQSClient(
                getEnv(SQS_ENDPOINT),
                getEnv(SQS_REGION)
        );

        String queueUrl = getEnv(SQS_QUEUE_URL);
        participantEventQueue = new SqsEventQueue(client, queueUrl);

        jsonObjectMapper = new ObjectMapper();
    }

    private static AmazonS3 createS3Client(String endpoint, String region) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builder = builder.withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new DefaultAWSCredentialsProviderChain());
        return builder.build();
    }

    private static AmazonSQS createSQSClient(String serviceEndpoint, String signingRegion) {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion);
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Override
    public String handleRequest(Map<String, Object> s3EventMap, Context context) {
        try {
            handleS3Event(S3BucketEvent.from(s3EventMap, jsonObjectMapper));
            return "OK";
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void handleS3Event(S3BucketEvent event) throws Exception {
        LOG.info("Process S3 event with: "+event);
        String participantId = event.getParticipantId();
        String challengeId = event.getChallengeId();

        Repository remoteRepo = remoteGithubClient.createNewRepositoryIfNotExists(
                challengeId, participantId);
        Git localRepo = localGitClient.cloneToTemp(remoteRepo.getCloneUrl());

        S3Object remoteSRCSFile = s3Client.getObject(event.getBucket(), event.getKey());
        srcsToGitExporter.export(remoteSRCSFile, localRepo);
        localGitClient.pushToRemote(localRepo);

        participantEventQueue.send(new SourceCodeUpdatedEvent(System.currentTimeMillis(),
                        participantId, challengeId, remoteRepo.getHtmlUrl()));
    }

}
