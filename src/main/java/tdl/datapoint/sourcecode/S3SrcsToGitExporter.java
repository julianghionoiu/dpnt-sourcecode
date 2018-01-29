package tdl.datapoint.sourcecode;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import tdl.record.sourcecode.snapshot.file.ToGitConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

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

    S3SrcsToGitExporter() {
    }

    public void export(S3Object s3Object, Git localRepo) throws Exception {
        Path inputFile = downloadObject(s3Object);
        Path outputDir = localRepo.getRepository()
                .getDirectory()
                .toPath();
        ToGitConverter converter = new ToGitConverter(inputFile, outputDir);
        converter.convert();
    }

    private Path downloadObject(S3Object s3Object) throws IOException {
        File file = File.createTempFile("code_", ".srcs");
        InputStream source = s3Object.getObjectContent();
        FileUtils.copyInputStreamToFile(source, file);
        return file.toPath();
    }

    public static AmazonS3 createDefaultS3Client() {
        String endpoint = System.getenv(ENV_S3_ENDPOINT);
        String region = System.getenv(ENV_S3_REGION);
        String accessKey = System.getenv(ENV_S3_ACCESS_KEY);
        String secretKey = System.getenv(ENV_S3_SECRET_KEY);

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builder = builder.withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKey, secretKey)));
        return builder.build();
    }

}
