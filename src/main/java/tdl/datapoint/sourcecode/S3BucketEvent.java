package tdl.datapoint.sourcecode;

import java.util.List;
import java.util.Map;

public class S3BucketEvent {
    private String bucket;

    private String key;

    private S3BucketEvent(String bucket, String key) {
        this.bucket = bucket;
        this.key = key;
    }

    @SuppressWarnings("unchecked")
    public static S3BucketEvent from(Map<String, Object> request) {
        Map<String, Object> record = ((List<Map<String, Object>>) request.get("Records")).get(0);
        Map<String, Object> s3 = (Map<String, Object>) record.get("s3");
        String  bucket = (String) ((Map<String, Object>) s3.get("bucket")).get("name");
        String key = (String) ((Map<String, Object>) s3.get("object")).get("key");
        return new S3BucketEvent(bucket, key);
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

}
