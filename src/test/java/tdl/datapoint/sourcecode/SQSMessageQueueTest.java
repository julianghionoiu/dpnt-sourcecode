package tdl.datapoint.sourcecode;

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
        String url = LocalSQSQueue.getQueueUrlOrCreate(queueName);
        SQSMessageQueue queue = new SQSMessageQueue();
        environmentVariables.set(SQSMessageQueue.ENV_SQS_QUEUE_URL, url);

        String messageId = queue.send("Hello!");
        assertNotNull(messageId);
    }
}
