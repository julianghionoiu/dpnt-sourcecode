package tdl.datapoint.sourcecode;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import tdl.record.sourcecode.snapshot.file.Segment;
import tdl.record.sourcecode.snapshot.file.ToGitConverter;

/**
 * This class exports file srcs at S3 repository to existing Git repository.
 * This will only be exported to local repository. Pushing to remote will not be
 * handled here.
 *
 * @author Petra Barus <petra.barus@gmail.com>
 */
public class S3SrcsToGitExporter {

    public static final String ENV_S3_ENDPOINT = "S3_ENDPOINT";

    public static final String ENV_S3_REGION = "S3_REGION";

    public static final String ENV_S3_ACCESS_KEY = "S3_ACCESS_KEY";

    public static final String ENV_S3_SECRET_KEY = "S3_SECRET_KEY";

    private final S3Object s3Object;

    private final Git git;

    S3SrcsToGitExporter(S3Object s3Object, Git git) {
        this.s3Object = s3Object;
        this.git = git;
    }

    public void export() throws Exception {
        Path inputFile = downloadObject();
        Path outputDir = getGitPath();
        ToGitConverter converter = new ToGitConverter(inputFile, outputDir);
        converter.convert();
    }

    private Path downloadObject() throws IOException {
        File file = File.createTempFile("code_", ".srcs");
        InputStream source = s3Object.getObjectContent();
        FileUtils.copyInputStreamToFile(source, file);
        return file.toPath();
    }

    public static AmazonS3 createDefaultS3Client() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        String endpoint = System.getenv(ENV_S3_ENDPOINT);
        String region = System.getenv(ENV_S3_REGION);
        if (endpoint != null && region != null) {
            AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
            builder = builder.withPathStyleAccessEnabled(true)
                    .withEndpointConfiguration(endpointConfiguration);
        }
        String accessKey = System.getenv(ENV_S3_ACCESS_KEY);
        String secretKey = System.getenv(ENV_S3_SECRET_KEY);
        if (accessKey != null && secretKey != null) {
            AWSCredentials credentialsProvider = new BasicAWSCredentials(accessKey, secretKey);
            builder = builder.withCredentials(new AWSStaticCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }

    private Path getGitPath() {
        return git.getRepository()
                .getDirectory()
                .toPath();
    }
}
