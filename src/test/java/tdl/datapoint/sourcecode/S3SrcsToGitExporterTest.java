package tdl.datapoint.sourcecode;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class S3SrcsToGitExporterTest {

    private static final String BUCKET = "localbucket";

    private static final String GIT_URI = "git://localhost:1234/";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void export() throws GitAPIException, Exception {
        AmazonS3 client = createS3Client();
        createBucketIfNotExists(client, BUCKET);
        Path path = Paths.get("src/test/resources/test.srcs");
        client.putObject(BUCKET, "test.srcs", path.toFile());

        Git git = Git.cloneRepository()
                .setURI(GIT_URI)
                .setDirectory(folder.newFolder())
                .call();

        long initSize = getLogCount(git);

        assertEquals(1, initSize);

        S3SrcsToGitExporter exporter = new S3SrcsToGitExporter(BUCKET, "test.srcs", GIT_URI, client);
        exporter.export();
        
        git.pull().call();
        
        long newSize = getLogCount(git);
        assertEquals(7, newSize);
    }

    private long getLogCount(Git git) {
        try {
            long size = 0;
            Iterator iterator = git.log().call().iterator();
            while (iterator.hasNext()) {
                size++;
                iterator.next();
            }
            return size;
        } catch (GitAPIException ex) {
            return 0;
        }
    }

    private void createBucketIfNotExists(AmazonS3 client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

    private AmazonS3 createS3Client() {
        EndpointConfiguration endpoint = new EndpointConfiguration("http://127.0.0.1:9000", "us-east-1");
        AWSCredentials credential = new BasicAWSCredentials("minio_access_key", "minio_secret_key");
        AmazonS3 amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(credential))
                .withEndpointConfiguration(endpoint)
                .build();
        return amazonS3;
    }
}
