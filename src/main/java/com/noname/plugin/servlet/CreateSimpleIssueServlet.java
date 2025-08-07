package com.noname.plugin.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.noname.plugin.api.ProjectFilter;
import com.noname.plugin.dto.SimpleIssueCreationLogDto;
import com.noname.plugin.impl.ProjectFilterImpl;
import com.noname.plugin.service.ProjectListManager;
import com.noname.plugin.service.SimpleIssueCreationLogService;
import com.noname.plugin.service.SimpleIssueCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dl
 * @date 18.06.2025 13:14
 */

public class CreateSimpleIssueServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CreateSimpleIssueServlet.class.getName());
    private final SimpleIssueCreationLogService CREATION_LOG_SERVICE;
    private static final ProjectFilter filter = new ProjectFilterImpl();
    private static final ProjectListManager plm = new ProjectListManager(Collections.singletonList(filter));
    private static final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

    @Inject
    public CreateSimpleIssueServlet(SimpleIssueCreationLogService simpleAo) {
        this.CREATION_LOG_SERVICE = checkNotNull(simpleAo);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Project> filteredProjects = plm.getFilteredProjects(user, getAllProjects());
        resp.setContentType("text/html; charset=utf-8");
        String formTemplate = readTemplate("templates/form-create-simple-issue.html");
        formTemplate = formTemplate.replace("<!--PROJECT_OPTIONS-->", projectsString(filteredProjects));
        resp.getWriter().write(formTemplate);
        resp.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String projectKey = req.getParameter("projectKey");
        String summary = req.getParameter("summary");
        String description = req.getParameter("description");

        SimpleIssueCreationService service = new SimpleIssueCreationService();

        Optional<String> result = service.createIssueFromForm(projectKey, summary, description);

        resp.setContentType("application/json; charset=utf-8");
        if (result.isPresent()) {
            logIssueCreated(projectKey, summary, result.get(), true);
            resp.getWriter().write("{\"status\":\"created\", \"key\":\"" + result.get() + "\"}");
        } else {
            logIssueCreated(projectKey, summary, null,false);
            resp.getWriter().write("{\"status\":\"error\", \"message\":\"Issue not created\"}");
        }
    }

    private String readTemplate(String path) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new FileNotFoundException("Template not found: " + path);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static List<Project> getAllProjects() {
        return plm.getAllProjects();
    }

    private String projectsString(List<Project> projectList) {
        StringBuilder result = new StringBuilder();
        for (Project p : projectList) {
            result.append("<option value=\"").append(p.getKey()).append("\">")
                    .append(p.getName())
                    .append(" (").append(p.getKey()).append(")")
                    .append("</option>\n");
        }

        return result.toString();
    }

    private void logIssueCreated(String projectKey,
                                 String summary,
                                 String issueKey,
                                 Boolean isCreated) {
        SimpleIssueCreationLogDto dto = new SimpleIssueCreationLogDto();
        dto.setAuthorKey(user.getKey());
        dto.setProjectKey(projectKey);
        dto.setSummary(summary);
        dto.setAttemptCreate(new Date());
        dto.setIssueCreated(isCreated);
        if (!isCreated) {
            String errorMessage = "Issue creation failed for project: " + projectKey + ", summary: " + summary;
            log.debug("Issue creation failed for project: {}, summary: {}", projectKey, summary);
            dto.setErrorMessage(errorMessage);
        }
        if (issueKey != null) {
            dto.setIssueKey(issueKey);
        }

        CREATION_LOG_SERVICE.logIssueCreation(dto);
    }
}
