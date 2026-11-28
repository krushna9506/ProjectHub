package com.projecthub;

import com.projecthub.models.GitHubItem;
import com.projecthub.repository.GitHubRepository;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitHubRepositoryTest {

    @Test
    public void parseOwnerRepo_handlesUrlsAndSlugs() {
        assertEquals("krushna9506/ProjectHub",
            GitHubRepository.parseOwnerRepo("https://github.com/krushna9506/ProjectHub"));
        assertEquals("krushna9506/ProjectHub",
            GitHubRepository.parseOwnerRepo("https://github.com/krushna9506/ProjectHub.git/"));
        assertEquals("octocat/Hello-World",
            GitHubRepository.parseOwnerRepo("octocat/Hello-World"));
        assertEquals("krushna9506/ProjectHub",
            GitHubRepository.parseOwnerRepo("https://github.com/krushna9506/ProjectHub?tab=readme#top"));
        assertEquals("", GitHubRepository.parseOwnerRepo("invalid-input"));
        assertEquals("", GitHubRepository.parseOwnerRepo("../etc/passwd"));
        assertEquals("", GitHubRepository.parseOwnerRepo("owner/.."));
        assertEquals("", GitHubRepository.parseOwnerRepo("owner/repo name"));
    }

    @Test
    public void parseIssuesJson_separatesIssuesAndPullRequests() throws Exception {
        String sampleJson = "["
            + "{\"number\": 12, \"title\": \"Fix auth\", \"state\": \"open\", \"html_url\": \"https://github.com/a/b/issues/12\", \"updated_at\": \"2026-11-28T10:00:00Z\", \"user\": {\"login\": \"alice\"}, \"labels\": [{\"name\": \"bug\"}]},"
            + "{\"number\": 15, \"title\": \"Add Kanban\", \"state\": \"closed\", \"html_url\": \"https://github.com/a/b/pull/15\", \"updated_at\": \"2026-11-28T11:00:00Z\", \"user\": {\"login\": \"bob\"}, \"pull_request\": {\"url\": \"https://api.github.com/repos/a/b/pulls/15\"}, \"labels\": []}"
            + "]";

        List<GitHubItem> items = GitHubRepository.parseIssuesJson(sampleJson);
        assertEquals(2, items.size());

        GitHubItem issue = items.get(0);
        assertFalse(issue.isPullRequest());
        assertEquals(12, issue.getNumber());
        assertEquals("alice", issue.getAuthor());
        assertEquals("bug", issue.getLabels().get(0));

        GitHubItem pr = items.get(1);
        assertTrue(pr.isPullRequest());
        assertEquals(15, pr.getNumber());
        assertEquals("closed", pr.getState());
    }
}
