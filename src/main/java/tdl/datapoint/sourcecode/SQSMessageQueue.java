package tdl.datapoint.sourcecode;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class SQSMessageQueue {

    public static final String ENV_SQS_ENDPOINT = "SQS_ENDPOINT";
    
    public static final String ENV_SQS_REGION = "SQS_REGION";
    
    public static final String ENV_SQS_QUEUE_URL = "SQS_QUEUE_URL";

    private final AmazonSQS client;

    public SQSMessageQueue(AmazonSQS client) {
        this.client = client;
    }

    SQSMessageQueue() {
        this.client = createSqsClient();
    }

    private static AmazonSQS createSqsClient() {
        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
        //Debt No env variables deeper than the Handler
        String endpoint = System.getenv(ENV_SQS_ENDPOINT);
        String region = System.getenv(ENV_SQS_REGION);
        if (endpoint != null && region != null) {
            AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
            builder = builder.withEndpointConfiguration(config);
        }
        return builder.build();
    }

    private String getQueueUrl() {
        return System.getenv(ENV_SQS_QUEUE_URL);
    }

    public String send(String message) {
        SendMessageRequest request = new SendMessageRequest(getQueueUrl(), message);
        SendMessageResult result = client.sendMessage(request);
        return result.getMessageId();
    }

}
