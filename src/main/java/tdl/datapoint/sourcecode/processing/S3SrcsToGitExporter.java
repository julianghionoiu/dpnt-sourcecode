package tdl.datapoint.sourcecode.processing;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import tdl.record.sourcecode.snapshot.file.ToGitConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * This class exports file srcs at S3 repository to existing Git repository.
 * This will only be exported to local repository. Pushing to remote will not be
 * handled here.
 *
 * @author Petra Barus <petra.barus@gmail.com>
 */
public class S3SrcsToGitExporter {

    public void export(S3Object s3Object, Git localRepo) throws Exception {
        Path inputFile = downloadObject(s3Object);
        Path outputDir = localRepo.getRepository()
                .getDirectory()
                .toPath();
        ToGitConverter converter = new ToGitConverter(inputFile, outputDir);
        converter.convert();
    }

    private Path downloadObject(S3Object s3Object) throws IOException {
        File file = File.createTempFile("code_", ".srcs");
        InputStream source = s3Object.getObjectContent();
        FileUtils.copyInputStreamToFile(source, file);
        return file.toPath();
    }
}
