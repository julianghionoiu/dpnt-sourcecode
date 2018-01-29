package tdl.datapoint.sourcecode.support;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class LocalS3Bucket {

    public static final String MINIO_URL = "http://127.0.0.1:9000";
    public static final String MINIO_REGION = "us-east-1";
    public static final String MINIO_ACCESS_KEY = "minio_access_key";
    public static final String MINIO_SECRET_KEY = "minio_secret_key";

    //Debt this should move into the Lambda code and inject the parameters
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
}
