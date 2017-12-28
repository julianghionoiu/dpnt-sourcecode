package tdl.datapoint.sourcecode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.egit.github.core.client.GitHubClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import org.eclipse.egit.github.core.Repository;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class SrcsGithubRepoTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void doesGithubRepoExistForKeyShouldReturnTrue() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"1234\"}"))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository", client);
        assertTrue(repo.doesGithubRepoExist());
    }

    @Test
    public void doesGithubRepoExistShouldReturnFalse() {
        stubFor(get(urlEqualTo("/api/v3/repos/user1/repository"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(400))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository", client);
        assertFalse(repo.doesGithubRepoExist());
    }
    @Test
    public void parseS3KeyToRepositoryName() {
        assertEquals("username", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/username/file.srcs"));
        assertEquals("user1", SrcsGithubRepo.parseS3KeyToRepositoryName("challenge/user1/file.srcs"));
    }

    @Test
    public void createNewRepository() throws IOException {
        String result = "{"
                + "  \"id\": 1296269,"
                + "  \"owner\": {"
                + "    \"login\": \"octocat\","
                + "    \"id\": 1,"
                + "    \"avatar_url\": \"https://github.com/images/error/octocat_happy.gif\","
                + "    \"gravatar_id\": \"\","
                + "    \"url\": \"https://api.github.com/users/octocat\","
                + "    \"html_url\": \"https://github.com/octocat\","
                + "    \"followers_url\": \"https://api.github.com/users/octocat/followers\","
                + "    \"following_url\": \"https://api.github.com/users/octocat/following{/other_user}\","
                + "    \"gists_url\": \"https://api.github.com/users/octocat/gists{/gist_id}\","
                + "    \"starred_url\": \"https://api.github.com/users/octocat/starred{/owner}{/repo}\","
                + "    \"subscriptions_url\": \"https://api.github.com/users/octocat/subscriptions\","
                + "    \"organizations_url\": \"https://api.github.com/users/octocat/orgs\","
                + "    \"repos_url\": \"https://api.github.com/users/octocat/repos\","
                + "    \"events_url\": \"https://api.github.com/users/octocat/events{/privacy}\","
                + "    \"received_events_url\": \"https://api.github.com/users/octocat/received_events\","
                + "    \"type\": \"User\","
                + "    \"site_admin\": false"
                + "  },"
                + "  \"name\": \"Hello-World\","
                + "  \"full_name\": \"octocat/Hello-World\","
                + "  \"description\": \"This your first repo!\","
                + "  \"private\": false,"
                + "  \"fork\": false,"
                + "  \"url\": \"https://api.github.com/repos/octocat/Hello-World\","
                + "  \"html_url\": \"https://github.com/octocat/Hello-World\","
                + "  \"archive_url\": \"http://api.github.com/repos/octocat/Hello-World/{archive_format}{/ref}\","
                + "  \"assignees_url\": \"http://api.github.com/repos/octocat/Hello-World/assignees{/user}\","
                + "  \"blobs_url\": \"http://api.github.com/repos/octocat/Hello-World/git/blobs{/sha}\","
                + "  \"branches_url\": \"http://api.github.com/repos/octocat/Hello-World/branches{/branch}\","
                + "  \"clone_url\": \"https://github.com/octocat/Hello-World.git\","
                + "  \"collaborators_url\": \"http://api.github.com/repos/octocat/Hello-World/collaborators{/collaborator}\","
                + "  \"comments_url\": \"http://api.github.com/repos/octocat/Hello-World/comments{/number}\","
                + "  \"commits_url\": \"http://api.github.com/repos/octocat/Hello-World/commits{/sha}\","
                + "  \"compare_url\": \"http://api.github.com/repos/octocat/Hello-World/compare/{base}...{head}\","
                + "  \"contents_url\": \"http://api.github.com/repos/octocat/Hello-World/contents/{+path}\","
                + "  \"contributors_url\": \"http://api.github.com/repos/octocat/Hello-World/contributors\","
                + "  \"deployments_url\": \"http://api.github.com/repos/octocat/Hello-World/deployments\","
                + "  \"downloads_url\": \"http://api.github.com/repos/octocat/Hello-World/downloads\","
                + "  \"events_url\": \"http://api.github.com/repos/octocat/Hello-World/events\","
                + "  \"forks_url\": \"http://api.github.com/repos/octocat/Hello-World/forks\","
                + "  \"git_commits_url\": \"http://api.github.com/repos/octocat/Hello-World/git/commits{/sha}\","
                + "  \"git_refs_url\": \"http://api.github.com/repos/octocat/Hello-World/git/refs{/sha}\","
                + "  \"git_tags_url\": \"http://api.github.com/repos/octocat/Hello-World/git/tags{/sha}\","
                + "  \"git_url\": \"git:github.com/octocat/Hello-World.git\","
                + "  \"hooks_url\": \"http://api.github.com/repos/octocat/Hello-World/hooks\","
                + "  \"issue_comment_url\": \"http://api.github.com/repos/octocat/Hello-World/issues/comments{/number}\","
                + "  \"issue_events_url\": \"http://api.github.com/repos/octocat/Hello-World/issues/events{/number}\","
                + "  \"issues_url\": \"http://api.github.com/repos/octocat/Hello-World/issues{/number}\","
                + "  \"keys_url\": \"http://api.github.com/repos/octocat/Hello-World/keys{/key_id}\","
                + "  \"labels_url\": \"http://api.github.com/repos/octocat/Hello-World/labels{/name}\","
                + "  \"languages_url\": \"http://api.github.com/repos/octocat/Hello-World/languages\","
                + "  \"merges_url\": \"http://api.github.com/repos/octocat/Hello-World/merges\","
                + "  \"milestones_url\": \"http://api.github.com/repos/octocat/Hello-World/milestones{/number}\","
                + "  \"mirror_url\": \"git:git.example.com/octocat/Hello-World\","
                + "  \"notifications_url\": \"http://api.github.com/repos/octocat/Hello-World/notifications{?since, all, participating}\","
                + "  \"pulls_url\": \"http://api.github.com/repos/octocat/Hello-World/pulls{/number}\","
                + "  \"releases_url\": \"http://api.github.com/repos/octocat/Hello-World/releases{/id}\","
                + "  \"ssh_url\": \"git@github.com:octocat/Hello-World.git\","
                + "  \"stargazers_url\": \"http://api.github.com/repos/octocat/Hello-World/stargazers\","
                + "  \"statuses_url\": \"http://api.github.com/repos/octocat/Hello-World/statuses/{sha}\","
                + "  \"subscribers_url\": \"http://api.github.com/repos/octocat/Hello-World/subscribers\","
                + "  \"subscription_url\": \"http://api.github.com/repos/octocat/Hello-World/subscription\","
                + "  \"svn_url\": \"https://svn.github.com/octocat/Hello-World\","
                + "  \"tags_url\": \"http://api.github.com/repos/octocat/Hello-World/tags\","
                + "  \"teams_url\": \"http://api.github.com/repos/octocat/Hello-World/teams\","
                + "  \"trees_url\": \"http://api.github.com/repos/octocat/Hello-World/git/trees{/sha}\","
                + "  \"homepage\": \"https://github.com\","
                + "  \"language\": null,"
                + "  \"forks_count\": 9,"
                + "  \"stargazers_count\": 80,"
                + "  \"watchers_count\": 80,"
                + "  \"size\": 108,"
                + "  \"default_branch\": \"master\","
                + "  \"open_issues_count\": 0,"
                + "  \"topics\": ["
                + "    \"octocat\","
                + "    \"atom\","
                + "    \"electron\","
                + "    \"API\""
                + "  ],"
                + "  \"has_issues\": true,"
                + "  \"has_wiki\": true,"
                + "  \"has_pages\": false,"
                + "  \"has_downloads\": true,"
                + "  \"archived\": false,"
                + "  \"pushed_at\": \"2011-01-26T19:06:43Z\","
                + "  \"created_at\": \"2011-01-26T19:01:12Z\","
                + "  \"updated_at\": \"2011-01-26T19:14:43Z\","
                + "  \"permissions\": {"
                + "    \"admin\": false,"
                + "    \"push\": false,"
                + "    \"pull\": true"
                + "  },"
                + "  \"allow_rebase_merge\": true,"
                + "  \"allow_squash_merge\": true,"
                + "  \"allow_merge_commit\": true,"
                + "  \"subscribers_count\": 42,"
                + "  \"network_count\": 0,"
                + "  \"has_projects\": true"
                + "}";

        stubFor(post(urlEqualTo("/api/v3/user/repos"))
                .withHeader("Accept", equalTo("application/vnd.github.beta+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(result))
        );
        environmentVariables.set("GITHUB_DEFAULT_USER", "user1");
        GitHubClient client = new GitHubClient("localhost", 8089, "http");
        SrcsGithubRepo repo = new SrcsGithubRepo("repository1", client);
        Repository newRepo = repo.createNewRepository();
        assertEquals("repository1", newRepo.getName());
    }
}
