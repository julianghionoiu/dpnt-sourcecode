package tdl.datapoint.sourcecode;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.jgit.api.Git;
import tdl.datapoint.sourcecode.processing.*;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.SourceCodeUpdatedEvent;

import java.util.Map;
import java.util.Optional;
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

    private static String getEnv(String key) {
        return Optional.ofNullable(System.getenv(key))
                .orElseThrow(()
                        -> new RuntimeException("[Startup] Environment variable " + key + " not set"));
    }

    SourceCodeUploadHandler() {
        s3Client = createS3Client(
                getEnv(S3_ENDPOINT),
                getEnv(S3_REGION),
                getEnv(S3_ACCESS_KEY),
                getEnv(S3_SECRET_KEY));

        remoteGithubClient = new RemoteGithub(
                getEnv(GITHUB_HOST),
                Integer.parseInt(getEnv(GITHUB_PORT)),
                getEnv(GITHUB_PROTOCOL),
                getEnv(GITHUB_AUTH_TOKEN),
                getEnv(GITHUB_REPO_OWNER));

        localGitClient = new LocalGitClient(
                getEnv(GITHUB_AUTH_TOKEN));

        srcsToGitExporter = new S3SrcsToGitExporter();


        AmazonSQS client = createSQSClient(
                getEnv(SQS_ENDPOINT),
                getEnv(SQS_REGION),
                getEnv(SQS_ACCESS_KEY),
                getEnv(SQS_SECRET_KEY)
        );

        String queueUrl = getEnv(SQS_QUEUE_URL);
        participantEventQueue = new SqsEventQueue(client, queueUrl);
    }

    private static AmazonS3 createS3Client(String endpoint, String region, String accessKey, String secretKey) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builder = builder.withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKey, secretKey)));
        return builder.build();
    }

    private static AmazonSQS createSQSClient(String serviceEndpoint, String signingRegion, String accessKey, String secretKey) {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion);
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();
    }

    @Override
    public String handleRequest(Map<String, Object> s3EventMap, Context context) {
        try {
            handleS3Event(S3BucketEvent.from(s3EventMap));
            return "OK";
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void handleS3Event(S3BucketEvent event) throws Exception {
        String participantId = event.getParticipantId();
        String challengeId = event.getChallengeId();
        Repository remoteRepo = remoteGithubClient.createNewRepositoryIfNotExists(
                challengeId, participantId);
        Git localRepo = localGitClient.cloneToTemp(remoteRepo.getCloneUrl());

        S3Object remoteSRCSFile = s3Client.getObject(event.getBucket(), event.getKey());
        srcsToGitExporter.export(remoteSRCSFile, localRepo);
        localGitClient.pushToRemote(localRepo);

        participantEventQueue.send(
                new SourceCodeUpdatedEvent(participantId, challengeId, remoteRepo.getCloneUrl()));
    }

}
