package tdl.datapoint.sourcecode.processing;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class SQSMessageQueue {

    private final AmazonSQS client;

    public SQSMessageQueue(String endpoint, String region) {
        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
        AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        builder = builder.withEndpointConfiguration(config);
        this.client = builder.build();
    }

    public void send(String message, String queueUrl) {
        SendMessageRequest request = new SendMessageRequest(queueUrl, message);
        SendMessageResult result = client.sendMessage(request);
        result.getMessageId();
    }

}
