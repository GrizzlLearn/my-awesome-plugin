package com.noname.plugin.service;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.project.ProjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

/**
 * @author dl
 * @date 18.06.2025 14:09
 */
public class SimpleIssueCreationService {
    private static final Logger log = LoggerFactory.getLogger(SimpleIssueCreationService.class.getName());
    private static final ProjectManager projectManager = ComponentAccessor.getProjectManager();

    /**
     *
     * @param projectKey - ключ проекта
     * @param summary - заголовок задачи
     * @param description - описание задачи
     * @return ключ задачи, иначе пустую строку
     */
    public Optional<String> createIssueFromForm(String projectKey, String summary, String description) {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        IssueService issueService = ComponentAccessor.getIssueService();

        IssueInputParameters input = issueService.newIssueInputParameters();
        input.setProjectId(getProjectIdByKey(projectKey))
                .setIssueTypeId("10000")
                .setSummary(summary)
                .setReporterId(user.getUsername())
                .setDescription(description);

        IssueService.CreateValidationResult validationResult = issueService.validateCreate(user, input);

        if (validationResult.isValid()) {
            IssueService.IssueResult result = issueService.create(user, validationResult);
            if (result.isValid()) {
                log.debug("Issue creation success: {}", result.getIssue().getKey());
                return Optional.of(result.getIssue().getKey());
            } else {
                log.debug("Issue creation failed: {}", result.getErrorCollection());
                return Optional.empty();
            }
        } else {
            log.debug("Validation failed: {}", validationResult.getErrorCollection());
            return Optional.empty();
        }
    }

    /**
     * Конвертирует ключ проекта в id проекта
     * @param projectKey
     * @return id проекта, иначе null
     */
    private Long getProjectIdByKey(String projectKey) {
        log.debug("extractValue(projectKey) -> {}", extractValue(projectKey));
        Long id = projectManager.getProjectObjByKey(projectKey).getId();
        if (id == null) {
            log.error("Project ID is null for key: {}", projectKey);
            return null;
        }
        return id;
    }

    private String extractValue(String input) {
        log.debug("extractValue -> {}", input);
        if (input == null) return "";

        int start = input.indexOf('(');
        int end = input.indexOf(')');

        if (start >= 0 && end > start) {
            log.debug("extractValue input.substring(start + 1, end).trim() -> {}", input.substring(start + 1, end).trim());
            return input.substring(start + 1, end).trim();
        }

        return "";
    }
}
