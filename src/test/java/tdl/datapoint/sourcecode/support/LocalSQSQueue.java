package tdl.datapoint.sourcecode.support;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import org.hamcrest.CoreMatchers;
import tdl.participant.queue.connector.QueueSize;
import tdl.participant.queue.connector.SqsEventQueue;

import static org.hamcrest.MatcherAssert.assertThat;

public class LocalSQSQueue {
    public static final String ELASTIC_MQ_URL = "http://localhost:9324";
    public static final String ELASTIC_MQ_REGION = "elasticmq";
    public static final String ELASTIC_MQ_ACCESS_KEY = "x";
    public static final String ELASTIC_MQ_SECRET_KEY = "y";
    public static final String ELASTIC_MQ_QUEUE_URL = "http://localhost:9324/queue/participant-events";

    public static SqsEventQueue createInstance() {
        AmazonSQS client = testAwsClient();
        String queueUrl = ELASTIC_MQ_QUEUE_URL;
        client.purgeQueue(new PurgeQueueRequest(queueUrl));
        SqsEventQueue sqsEventQueue = new SqsEventQueue(client, queueUrl);
        assertThat("Queue "+ queueUrl +" is not clean.", sqsEventQueue.getQueueSize(),
                CoreMatchers.is(new QueueSize(0, 0, 0)));
        return sqsEventQueue;
    }

    private static AmazonSQS testAwsClient() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(ELASTIC_MQ_URL, ELASTIC_MQ_REGION);
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(ELASTIC_MQ_ACCESS_KEY, ELASTIC_MQ_SECRET_KEY)))
                .build();
    }

}
