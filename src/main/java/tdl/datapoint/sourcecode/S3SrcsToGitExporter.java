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
import org.eclipse.jgit.transport.CredentialsProvider;
import tdl.record.sourcecode.snapshot.file.Segment;
import tdl.record.sourcecode.snapshot.file.ToGitConverter;

/**
 * This class exports file srcs at S3 repository to existing Git repository.
 * This will only be exported to local repository. Pushing to remote will not
 * be handled here.
 *
 * @author Petra Barus <petra.barus@gmail.com>
 */
public class S3SrcsToGitExporter {

    private final S3Object s3Object;

    private final Git git;

    public S3SrcsToGitExporter(S3Object s3Object, Git git) {
        this.s3Object = s3Object;
        this.git = git;
    }

    public void export() throws GitAPIException, IOException, Exception {
        Path inputFile = downloadObject();
        Path outputDir = getGitPath();
        ToGitConverter converter = new ToGitConverter(inputFile, outputDir);
        converter.convert();
    }

    public void pushRemote() throws GitAPIException {
        git.push().call();
    }

    public Path downloadObject() throws IOException {
        File file = File.createTempFile("code_", ".srcs");
        InputStream source = s3Object.getObjectContent();
        FileUtils.copyInputStreamToFile(source, file);
        return file.toPath();
    }

    public static AmazonS3 createDefaultS3Client() {
        return AmazonS3ClientBuilder.standard()
                .build();
    }

    public Path getGitPath() {
        return git.getRepository()
                .getDirectory()
                .toPath();
    }
}
