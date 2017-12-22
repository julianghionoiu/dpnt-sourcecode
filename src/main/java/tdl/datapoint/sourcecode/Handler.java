package tdl.datapoint.sourcecode;

import java.util.Map;

import org.apache.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Handler implements RequestHandler<Map<String, Object>, Response> {

    private static final Logger LOG = Logger.getLogger(Handler.class);

    @Override
    public Response handleRequest(Map<String, Object> input, Context context) {
        Event event = new Event(input);
        //LOG.info("received: " + input);
        LOG.info("object: " + event.getBucket());
        LOG.info("bucket: " + event.getKey());
        Response response = new Response("ok");
        return response;
    }
}
