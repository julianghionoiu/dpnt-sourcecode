package tdl.datapoint.sourcecode;

import org.junit.Test;

public class SourcecodeDatapointAcceptanceTest {

    @Test
    public void create_repo_and_uploads_commits() throws Exception {
        /**
         * Setup:
         *  - an existing S3 bucket   (Minio)
         *  - no initial repo
         *  - an SQS Event queue      (ElasticMq)
         */

        /**
         * Input:
         *
         * Generate a new, unique username
         * Upload of a SRCS file to the bucket. The key will be something like `challenge/username/file.srcs`
         * Simulate the triggering of the lambda via an S3 event. (the input should be the KEY of the newly updated file)
         */

        /**
         * Notes on the implementation:
         *  - the lambda receives an event from S3, for a key like "challenge/username/file.srcs"
         *  - based on the key of file we construct a Github URL like: https://github.com/Challenge/username
         *     Example: https://github.com/TST-challenge/username
         *  - if the repo does not exist, we create the new repo
         *  - we clone the repo locally and we add the commits
         *  - we push the commits
         *  - we push the URL as an event to the SQS Queue
         */

        /**
         * Output (the assertions):
         *
         * An event should be publish with a REPO url on the SQS Event Queue. (check ElasticMq)
         * We clone the REPO, it should contain the expected commits
         */
    }


    @Test
    public void push_commits_to_existing_repo() throws Exception {
        /**
         * Same as the previous test, the only difference is that the target repo already exists: https://github.com/Challenge/username
         */
    }
}
