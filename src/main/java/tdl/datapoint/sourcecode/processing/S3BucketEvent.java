package tdl.datapoint.sourcecode.processing;

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
        if (request == null) {
            throw new IllegalArgumentException("No input provided");
        }

        Map<String, Object> record = ((List<Map<String, Object>>) mapGet(request, "Records")).get(0);
        Map<String, Object> s3 = (Map<String, Object>) mapGet(record, "s3");
        String  bucket = (String) mapGet((Map<String, Object>) s3.get("bucket"), "name");
        String key = (String) mapGet((Map<String, Object>) s3.get("object"), "key");
        return new S3BucketEvent(bucket, key);
    }

    private static Object mapGet(Map<String, Object> map, String key) {
        if (map == null) {
            throw new IllegalArgumentException("No input provided. Map is \"null\".");
        }

        Object o = map.get(key);
        if (o == null) {
            throw new IllegalArgumentException(String.format("Key \"%s\" not found in map.", key));
        }
        return o;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public String getChallengeId() {
        return key.split("/")[0];
    }

    public String getParticipantId() {
        return key.split("/")[1];
    }

    @Override
    public String toString() {
        return "S3BucketEvent{" +
                "bucket='" + bucket + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
