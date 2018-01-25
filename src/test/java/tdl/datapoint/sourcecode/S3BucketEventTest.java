package tdl.datapoint.sourcecode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

public class S3BucketEventTest {

    @Test
    public void constructFromJson() throws IOException {
        String json = "{\"Records\":[{\"s3\":{\"bucket\":{\"name\":\"localbucket\"}, \"object\":{\"key\":\"path\"}}}]}";
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> map;
        
        map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        
        S3BucketEvent event = new S3BucketEvent(map);
        assertEquals(event.getBucket(), "localbucket");
        assertEquals(event.getKey(), "path");
    }
}
