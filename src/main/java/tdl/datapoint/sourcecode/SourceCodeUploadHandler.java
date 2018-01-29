package tdl.datapoint.sourcecode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.jgit.api.Git;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceCodeUploadHandler implements RequestHandler<Map<String, Object>, String> {
    private static final Logger LOG = Logger.getLogger(SourceCodeUploadHandler.class.getName());
    private AmazonS3 s3Client;
    private RemoteGithub remoteGithubClient;
    private LocalGitClient localGitClient;
    private SQSMessageQueue queue;
    private S3SrcsToGitExporter srcsToGitExporter;

    SourceCodeUploadHandler() {
        s3Client = S3SrcsToGitExporter.createDefaultS3Client();

        String githubHost = System.getenv(RemoteGithub.ENV_GITHUB_HOST);
        int githubPort = Integer.parseInt(System.getenv(RemoteGithub.ENV_GITHUB_PORT));
        String githubProtocol = System.getenv(RemoteGithub.ENV_GITHUB_PROTOCOL);
        String githubAuthToken = System.getenv(RemoteGithub.ENV_GITHUB_AUTH_TOKEN);
        String githubRepoOwner = System.getenv(RemoteGithub.ENV_GITHUB_REPO_OWNER);

        remoteGithubClient = new RemoteGithub(
                githubHost, githubPort, githubProtocol, githubAuthToken,
                githubRepoOwner);

        localGitClient = new LocalGitClient(githubAuthToken);

        srcsToGitExporter = new S3SrcsToGitExporter();

        queue = new SQSMessageQueue();
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
        Repository remoteRepo = remoteGithubClient.createNewRepositoryIfNotExists(
                event.getChallengeId(), event.getParticipantId());
        Git localRepo = localGitClient.cloneToTemp(remoteRepo.getCloneUrl());

        S3Object remoteSRCSFile = s3Client.getObject(event.getBucket(), event.getKey());
        srcsToGitExporter.export(remoteSRCSFile, localRepo);
        localGitClient.pushToRemote(localRepo);

        queue.send(remoteRepo.getCloneUrl());
    }

}
