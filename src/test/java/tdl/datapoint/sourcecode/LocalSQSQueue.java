package tdl.datapoint.sourcecode;

import com.amazonaws.SdkClientException;
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

public class LocalSQSQueue {
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
        } catch (SdkClientException e) {
            throw new IllegalStateException("SQS Service probably not running", e);
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
