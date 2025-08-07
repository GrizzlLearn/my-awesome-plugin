package com.noname.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

import java.util.Date;

/**
 * @author dl
 * @date 19.06.2025 13:32
 */
@Preload
@Table("SIMPLE_ISSUE_TABLE")
public interface SimpleIssueCreationLog extends Entity {
    String getProjectKey();
    void setProjectKey(String projectKey);

    String getSummary();
    void setSummary(String summary);

    String getIssueKey();
    void setIssueKey(String issueKey);

    String getAuthorKey();
    void setAuthorKey(String authorKey);

    Date getCreatedAt();
    void setCreatedAt(Date createdAt);

    Boolean getIssueIsCreated();
    void setIssueIsCreated(Boolean isCreated);

    String getErrorMessage();
    void setErrorMessage(String message);
}
