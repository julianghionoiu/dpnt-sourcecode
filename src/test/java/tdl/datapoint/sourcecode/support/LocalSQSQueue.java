package tdl.datapoint.sourcecode.support;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class LocalSQSQueue {
    public static final String ELASTIC_MQ_URL = "http://localhost:9324";
    public static final String ELASTIC_MQ_REGION = "elasticmq";
    public static final String ELASTIC_MQ_SQS_QUEUE_URL = "http://localhost:9324/queue/participant-events";
    private AmazonSQS client;
    private final String queueUrl;

    private LocalSQSQueue(AmazonSQS sqsClient, String queueUrl) {
        client = sqsClient;
        this.queueUrl = queueUrl;
    }


    public static LocalSQSQueue createInstance() {
        AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(ELASTIC_MQ_URL, ELASTIC_MQ_REGION);
        AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(config)
                .build();
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest(ELASTIC_MQ_SQS_QUEUE_URL);
        sqsClient.purgeQueue(purgeQueueRequest);

        return new LocalSQSQueue(sqsClient, ELASTIC_MQ_SQS_QUEUE_URL);
    }

    public  String getFirstMessageBody() {
        ReceiveMessageRequest rmr = new ReceiveMessageRequest(queueUrl);
        ReceiveMessageResult result = client.receiveMessage(rmr);
        
        
        String body = result.getMessages().get(0).getBody();
        
        String receiptHandle = result.getMessages().get(0).getReceiptHandle();
        DeleteMessageRequest dmr = new DeleteMessageRequest(queueUrl, receiptHandle);
        client.deleteMessage(dmr);
        
        return body;
    }

}
