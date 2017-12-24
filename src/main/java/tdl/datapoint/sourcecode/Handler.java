package tdl.datapoint.sourcecode;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.logging.Level;

public class Handler implements RequestHandler<Map<String, Object>, Response> {

    //private static final Logger LOG = Logger.getLogger(Handler.class);
    @Override
    public Response handleRequest(Map<String, Object> input, Context context) {
        try {
            S3BucketEvent event = new S3BucketEvent(input);
            SrcsGithubRepo repo = new SrcsGithubRepo(event.getKey());
            S3SrcsToGitExporter exporter = new S3SrcsToGitExporter(event.getBucket(), event.getKey(), repo.getUri());
            exporter.export();
            return new Response("ok");
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            return new Response("error: " + ex.getMessage());
        }
    }

}
