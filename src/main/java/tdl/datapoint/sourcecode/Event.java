package tdl.datapoint.sourcecode;

import java.util.List;
import java.util.Map;

public class Event {

    private final Map<String, Object> request;

    private String bucket;

    private String key;

    public Event(Map<String, Object> request) {
        this.request = request;
        parseRequest();
    }

    private void parseRequest() {
        Map<String, Object> record = ((List<Map<String, Object>>) request.get("Records")).get(0);
        Map<String, Object> s3 = (Map<String, Object>) record.get("s3");
        key = (String) ((Map<String, Object>) s3.get("object")).get("key");
        bucket = (String) ((Map<String, Object>) s3.get("bucket")).get("name");
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

}
