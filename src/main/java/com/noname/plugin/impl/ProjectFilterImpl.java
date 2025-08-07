package com.noname.plugin.impl;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.noname.plugin.api.ProjectFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dl
 * @date 18.06.2025 20:20
 */
public class ProjectFilterImpl implements ProjectFilter {
    private final PermissionManager permissionManager = ComponentAccessor.getPermissionManager();

    @Override
    public List<Project> filterCreateIssue(List<Project> input, ApplicationUser user) {
        return input.stream()
                .filter(project -> permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, project, user))
                .collect(Collectors.toList());
    }

}
