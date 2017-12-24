package tdl.datapoint.sourcecode;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import tdl.record.sourcecode.snapshot.file.Segment;
import tdl.record.sourcecode.snapshot.file.ToGitConverter;

/**
 * This class exports file srcs at S3 repository to existing Git repository.
 *
 * @author Petra Barus <petra.barus@gmail.com>
 */
public class S3SrcsToGitExporter {

    private final String bucket;

    private final String key;

    private final String gitUri;

    private final AmazonS3 s3;

    private Git git;

    public S3SrcsToGitExporter(String bucket, String key, String gitUri) {
        this.bucket = bucket;
        this.key = key;
        this.gitUri = gitUri;
        this.s3 = createDefaultS3Client();
    }

    public S3SrcsToGitExporter(String bucket, String key, String gitUri, AmazonS3 s3) {
        this.bucket = bucket;
        this.key = key;
        this.gitUri = gitUri;
        this.s3 = s3;
    }

    public void export() throws GitAPIException, IOException, Exception {
        Path inputFile = downloadObject();
        Path outputDir = cloneGit();
        ToGitConverter converter = new ToGitConverter(inputFile, outputDir);
        converter.convert();
        pushRemote();
    }

    public void pushRemote() throws GitAPIException {
        git.push().call();
    }

    public Path downloadObject() throws IOException {
        File file = File.createTempFile("code_", ".srcs");
        InputStream source = getS3Object();
        FileUtils.copyInputStreamToFile(source, file);
        return file.toPath();
    }

    private AmazonS3 createDefaultS3Client() {
        return AmazonS3ClientBuilder.standard()
                .build();
    }

    private InputStream getS3Object() throws IOException {
        GetObjectRequest request = new GetObjectRequest(bucket, key);
        S3Object object = s3.getObject(request);
        return object.getObjectContent();
    }

    public Path cloneGit() throws IOException, GitAPIException {
        Path path = createTempDir();
        git = Git.cloneRepository()
                .setURI(gitUri)
                .setDirectory(path.toFile())
                .call();
        return path;
    }

    private Path createTempDir() throws IOException {
        return Files.createTempDirectory("tmp");
    }
}
