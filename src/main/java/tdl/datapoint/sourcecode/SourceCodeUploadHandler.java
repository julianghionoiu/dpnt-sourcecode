package tdl.datapoint.sourcecode;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.api.Git;

public class SourceCodeUploadHandler implements RequestHandler<Map<String, Object>, String> {
    private static final Logger LOG = Logger.getLogger(SourceCodeUploadHandler.class.getName());
    private AmazonS3 s3Client;

    SourceCodeUploadHandler() {
        s3Client = S3SrcsToGitExporter.createDefaultS3Client();

        // S3
        // Github repo
        // SQS queue
    }

    @Override
    public String handleRequest(Map<String, Object> s3EventMap, Context context) {
        try {
            S3BucketEvent event = S3BucketEvent.from(s3EventMap);

            uploadCommitToRepo(event);
            return "OK";
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    //Debt should be private and we should construct the Map as AWS does
    private void uploadCommitToRepo(S3BucketEvent event) throws Exception {
        S3Object s3Object = s3Client.getObject(event.getBucket(), event.getKey());

        String repoName = RemoteGithub.parseS3KeyToRepositoryName(event.getKey());

        GitHubClient githubClient = RemoteGithub.createGithubClient();
        RemoteGithub githubApi = new RemoteGithub(
                System.getenv(RemoteGithub.ENV_GITHUB_USERNAME), githubClient);


        LocalGitClient localGitClient = new LocalGitClient(System.getenv(RemoteGithub.ENV_GITHUB_TOKEN));
        Repository remoteRepo = githubApi.createNewRepositoryIfNotExists(repoName);

        Git localRepo = localGitClient.cloneToTemp(remoteRepo.getCloneUrl());

        S3SrcsToGitExporter exporter = new S3SrcsToGitExporter(s3Object, localRepo);
        exporter.export();

        localGitClient.pushToRemote(localRepo);

        sendGithubUrlToQueue(githubApi.getUri());
    }

    private void sendGithubUrlToQueue(String url) {
        SQSMessageQueue queue = new SQSMessageQueue();
        queue.send(url);
    }

}
