package tdl.datapoint.sourcecode;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.eclipse.egit.github.core.client.GitHubClient;

public class ServiceMock {

    public static final String MINIO_URL = "http://127.0.0.1:9000";

    public static final String MINIO_REGION = "us-east-1";

    public static final String MINIO_ACCESS_KEY = "minio_access_key";

    public static final String MINIO_SECRET_KEY = "minio_secret_key";

    public static AmazonS3 createS3Client() {
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(MINIO_URL, MINIO_REGION);
        AWSCredentials credential = new BasicAWSCredentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY);
        AmazonS3 amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(credential))
                .withEndpointConfiguration(endpoint)
                .build();
        return amazonS3;
    }

    public static final String ELASTIC_MQ_URL = "http://localhost:9324";

    public static final String ELASTIC_MQ_REGION = "elasticmq";

    public static AmazonSQS createSQSClient() {
        AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(ELASTIC_MQ_URL, ELASTIC_MQ_REGION);
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(config)
                .build();
    }
    
    public static final String GITHUB_CLIENT_HOST = "localhost";
    
    public static final int GITHUB_CLIENT_PORT = 8089;
    
    public static final String GITHUB_CLIENT_PROTOCOL = "http";

    public static GitHubClient createGithubClient() {
        return new GitHubClient(GITHUB_CLIENT_HOST, GITHUB_CLIENT_PORT, GITHUB_CLIENT_PROTOCOL);
    }
}
