package tdl.datapoint.sourcecode;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class SQSMessageQueueTest {

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void sendMessage() {
        AmazonSQS client = ServiceMock.createSQSClient();
        String queueName = "queue2";
        GetQueueUrlResult result;
        try {
            result = client.getQueueUrl(queueName);
        } catch (QueueDoesNotExistException e) {
            client.createQueue(queueName);
            result = client.getQueueUrl(queueName);
        }

        SQSMessageQueue queue = new SQSMessageQueue(client);
        environmentVariables.set("SQS_QUEUE_URL", result.getQueueUrl());

        String messageId = queue.send("Hello!");
        assertNotNull(messageId);
    }
}
