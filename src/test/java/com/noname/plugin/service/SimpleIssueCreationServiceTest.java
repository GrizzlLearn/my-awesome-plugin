package com.noname.plugin.service;

import com.atlassian.jira.issue.Issue;
import org.junit.Test;
import com.atlassian.jira.component.ComponentAccessor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author dl
 * @date 18.06.2025 16:34
 */
public class SimpleIssueCreationServiceTest {
    @Test
    public void test() {
        SimpleIssueCreationService si = new SimpleIssueCreationService();
        String projectKey = "TEST";
        String summary = "TEST-20";
        String description = "TEST-20";
        Optional<String> result = si.createIssueFromForm(projectKey, summary, description);
        assertEquals(true, result.isPresent());
    }
}
