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
        String queueName = "queue2";
        String url = ServiceMock.getQueueUrlOrCreate(queueName);
        SQSMessageQueue queue = new SQSMessageQueue();
        environmentVariables.set("SQS_QUEUE_URL", url);

        String messageId = queue.send("Hello!");
        assertNotNull(messageId);
    }
}
