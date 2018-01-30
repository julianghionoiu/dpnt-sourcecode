package tdl.datapoint.sourcecode.support;

import tdl.record.sourcecode.snapshot.file.Header;
import tdl.record.sourcecode.snapshot.file.Reader;
import tdl.record.sourcecode.snapshot.file.Segment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestSrcsFile {
    private final Path resourcePath;

    public TestSrcsFile(String name) {
        resourcePath = Paths.get("src/test/resources/", name);
    }

    public File asFile() {
        return resourcePath.toFile();
    }

    public List<String> getCommitMessages() throws IOException {
        Reader reader = new Reader(asFile());
        List<String> messages = new ArrayList<>();
        while (reader.hasNext()) {
            Header header = reader.getFileHeader();
            Segment segment = reader.nextSegment();
            Date timestamp = new Date((header.getTimestamp() + segment.getTimestampSec()) * 1000L);
            String message = timestamp.toString();
            messages.add(message);
        }
        return messages;
    }

    public List<String> getTags() throws IOException {
        Reader reader = new Reader(asFile());
        List<String> tags = new ArrayList<>();
        while (reader.hasNext()) {
            Segment segment = reader.nextSegment();
            String tag = segment.getTag().trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
