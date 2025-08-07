package com.noname.plugin.api;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;

import java.util.List;

/**
 * @author dl
 * @date 18.06.2025 20:19
 */
public interface ProjectFilter {
    List<Project> filterCreateIssue(List<Project> input, ApplicationUser user);
    //TODO дописать другие фильтры, например удаление задачи, перенос задачи и тд.
}
