package tdl.datapoint.sourcecode.support;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.File;

public class LocalS3Bucket {

    public static final String MINIO_URL = "http://127.0.0.1:9000";
    public static final String MINIO_REGION = "us-east-1";
    public static final String MINIO_ACCESS_KEY = "minio_access_key";
    public static final String MINIO_SECRET_KEY = "minio_secret_key";
    private static final String BUCKET = "localbucket";

    public static String putObject(File object, String key) {
        AmazonS3 s3Client = LocalS3Bucket.createS3Client();
        createBucketIfNotExists(s3Client, BUCKET);
        s3Client.putObject(BUCKET, key, object);
        return "{\"Records\":[{\"s3\":{\"bucket\":{\"name\":\"" + BUCKET + "\"},"
                + " \"object\":{\"key\":\"" + key + "\"}}}]}";
    }

    @SuppressWarnings("deprecation")
    private static void createBucketIfNotExists(AmazonS3 client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

    private static AmazonS3 createS3Client() {
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(MINIO_URL, MINIO_REGION);
        AWSCredentials credential = new BasicAWSCredentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY);
        return AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(credential))
                .withEndpointConfiguration(endpoint)
                .build();
    }
}
