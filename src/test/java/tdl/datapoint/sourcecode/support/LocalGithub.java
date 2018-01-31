package tdl.datapoint.sourcecode.support;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class LocalGithub {
    public static final String GITHUB_HOST = "localhost";
    public static final String GITHUB_PORT = "9556";
    public static final String GITHUB_PROTOCOL = "http";
    public static final String GITHUB_ORGANISATION = "myorg";
    public static final String GITHUB_TOKEN = "test";

    public static List<String> getCommitMessages(String gitRepoUrl) throws Exception {
        Git git = checkout(gitRepoUrl);
        List<String> messages = new ArrayList<>();
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            messages.add(commit.getFullMessage());
        }
        Collections.reverse(messages);
        return messages;
    }

    private static Git checkout(String htmlUrl) throws IOException, URISyntaxException {
        String cloneUrl;
        cloneUrl = !htmlUrl.endsWith(".git") ? htmlUrl + ".git" : htmlUrl;
        return Git.open(new File(new URI(cloneUrl)));
    }

    public static List<String> getTags(String gitRepoUrl) throws Exception {
        Git git = checkout(gitRepoUrl);
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
