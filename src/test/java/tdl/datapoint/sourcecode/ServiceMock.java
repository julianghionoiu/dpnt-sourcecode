package tdl.datapoint.sourcecode;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

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

    private static AmazonSQS createSQSClient() {
        AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(ELASTIC_MQ_URL, ELASTIC_MQ_REGION);
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(config)
                .build();
    }

    public static String getQueueUrlOrCreate(String queueName) {
        AmazonSQS client = createSQSClient();
        GetQueueUrlResult result;
        try {
            result = client.getQueueUrl(queueName);
        } catch (QueueDoesNotExistException e) {
            client.createQueue(queueName);
            result = client.getQueueUrl(queueName);
        }
        return result.getQueueUrl();
    }

    public static void purgeQueue(String queueName) {
        AmazonSQS client = createSQSClient();
        String queueUrl = getQueueUrlOrCreate(queueName);
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest(queueUrl);
        client.purgeQueue(purgeQueueRequest);
    }

    public static String getFirstMessageBody(String queueUrl) {
        AmazonSQS client = createSQSClient();
        ReceiveMessageRequest rmr = new ReceiveMessageRequest(queueUrl);
        ReceiveMessageResult result = client.receiveMessage(rmr);
        
        
        String body = result.getMessages().get(0).getBody();
        
        String receiptHandle = result.getMessages().get(0).getReceiptHandle();
        DeleteMessageRequest dmr = new DeleteMessageRequest(queueUrl, receiptHandle);
        client.deleteMessage(dmr);
        
        return body;
    }
}
