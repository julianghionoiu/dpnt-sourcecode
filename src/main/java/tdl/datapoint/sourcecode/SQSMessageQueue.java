package tdl.datapoint.sourcecode;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class SQSMessageQueue {

    private final AmazonSQS client;

    public SQSMessageQueue(AmazonSQS client) {
        this.client = client;
    }

    public SQSMessageQueue() {
        this.client = createDefaultSqsClient();
    }

    private AmazonSQS createDefaultSqsClient() {
        return AmazonSQSClientBuilder.standard()
                .build();
    }

    private String getQueueUrl() {
        return System.getenv("SQS_QUEUE_URL");
    }

    public String send(String message) {
        SendMessageRequest request = new SendMessageRequest(getQueueUrl(), message);
        SendMessageResult result = client.sendMessage(request);
        return result.getMessageId();
    }

}
