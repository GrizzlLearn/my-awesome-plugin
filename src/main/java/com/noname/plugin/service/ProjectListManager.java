package com.noname.plugin.service;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.noname.plugin.api.ProjectFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dl
 * @date 18.06.2025 19:55
 */
public class ProjectListManager {
    private final ProjectManager pm = ComponentAccessor.getProjectManager();
    private final List<ProjectFilter> filters;

    public ProjectListManager(List<ProjectFilter> filters) {
        this.filters = filters;
    }

    public List<Project> getAllProjects() {
        List<Project> result = pm.getProjects();

        return result != null ? result : new ArrayList<>();
    }

    public List<Project> getFilteredProjects(ApplicationUser user, List<Project> initial) {
        List<Project> result = initial;
        for (ProjectFilter filter : filters) {
            result = filter.filterCreateIssue(result, user);
        }

        return result;
    }
}
