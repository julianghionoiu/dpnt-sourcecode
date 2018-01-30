package tdl.datapoint.sourcecode.support;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.net.URI;
import java.util.*;

public class LocalGithub {
    public static final String GITHUB_REPO_OWNER = "dpnttest";
    public static final String GITHUB_TOKEN = "test";
    public static final String GITHUB_HOST = "localhost";
    public static final String GITHUB_PORT = "9556";
    public static final String GITHUB_PROTOCOL = "http";

    public static List<String> getCommitMessages(String gitRepoUrl) throws Exception {
        Git git = Git.open(new File(new URI(gitRepoUrl)));
        List<String> messages = new ArrayList<>();
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            messages.add(commit.getFullMessage());
        }
        Collections.reverse(messages);
        return messages;
    }

    public static List<String> getTags(String gitRepoUrl) throws Exception {
        Git git = Git.open(new File(new URI(gitRepoUrl)));
        Repository repository = git.getRepository();
        List<Ref> tagRefs = git.tagList().call();
        Map<String, String> commitsToTags = new HashMap<>();

        // Create map of tags
        for (Ref tagRef : tagRefs) {
            String key = repository.peel(tagRef).getPeeledObjectId().getName();
            String value = tagRef.getName().replaceAll("refs/tags/", "");
            commitsToTags.put(key, value);
        }

        // Resolve the commits
        List<String> tags = new ArrayList<>();
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            String key = commit.getId().name();
            if (commitsToTags.containsKey(key)) {
                tags.add(commitsToTags.get(key));
            }
        }
        Collections.reverse(tags);

        return tags;
    }
}
