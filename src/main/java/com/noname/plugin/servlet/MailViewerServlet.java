package com.noname.plugin.servlet;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.noname.plugin.model.MailItem;
import com.noname.plugin.service.MailItemService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dl
 * @date 24.06.2025 22:54
 */
public class MailViewerServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(MailViewerServlet.class.getName());
    private final MailItemService mailItemService;

    @ComponentImport
    private final WebResourceManager webResourceManager;

    @Inject
    public MailViewerServlet(MailItemService mailItemService,
                             WebResourceManager webResourceManager) {
        this.mailItemService = checkNotNull(mailItemService);
        this.webResourceManager = checkNotNull(webResourceManager);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Инициализация тестовых данных если список пуст
        if (mailItemService.getAllMailItems().isEmpty()) {
            createMailItem("qwer", Collections.singletonList("qwer"), "qwer", "qwer");
            createMailItem("qwer1", Collections.singletonList("qwer1"), "qwer1", "qwer1");
            createMailItem("qwer2", Collections.singletonList("qwer2"), "qwer2", "qwer2");
            createMailItem("qwer3", Collections.singletonList("qwer3"), "qwer3", "qwer3");
        }

        String requestURI = req.getRequestURI();

        if (requestURI.endsWith("/mail-items")) {
            resp.sendRedirect(requestURI + "/");
            return;
        }

        // Корневой эндпоинт - главная страница с навигацией
        if (requestURI.endsWith("/mail-items/") || requestURI.endsWith("/mail-items")) {
            serveMainPage(resp);
            return;
        }

        // Эндпоинт для получения данных в JSON формате
        if (requestURI.endsWith("/mail-items/data")) {
            serveJsonData(resp);
            return;
        }

        // Эндпоинт для отображения страницы с таблицей
        if (requestURI.endsWith("/mail-items/table")) {
            serveTablePageWithWebResourceManager(req, resp);
            return;
        }

        // Обслуживание CSS файлов
        if (requestURI.endsWith("/css/mail-main.css")) {
            serveCssFile(resp, "css/mail-main.css");
            return;
        }

        if (requestURI.endsWith("/css/mail-table.css")) {
            serveCssFile(resp, "css/mail-table.css");
            return;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserManager userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager.class);
        UserKey userKey = userManager.getRemoteUserKey();

        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null) {
            resp.sendRedirect("/jira/login.jsp");
        }

        if (!userManager.isSystemAdmin(userKey)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType(MediaType.APPLICATION_JSON);
            resp.getWriter().write("{\"success\":false,\"error\":\"Access denied: Admin rights required\"}");
            return;
        }

        String pathInfo = req.getPathInfo();

        if ("/delete-all".equals(pathInfo)) {
            handleDeleteAll(resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType(MediaType.APPLICATION_JSON);
            resp.getWriter().write("{\"success\":false,\"error\":\"Endpoint not found\"}");
        }
    }

    private void serveMainPage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");

        InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("templates/mail-main.html");
        if (htmlStream != null) {
            resp.getWriter().write(generateStringBuffer(htmlStream).toString());
        } else {
            String navigationHtml = generateNavigationPage();
            resp.getWriter().write(navigationHtml);
        }
    }

    private void serveJsonData(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        try {
            resp.getWriter().write(mailItemService.getAllMailItemsAsJson());
        } catch (JSONException e) {
            log.error("Error converting mail items to JSON", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing data");
        }
    }

    /**
     * Простое решение: читаем VM файл и подключаем CSS через WebResourceManager
     */
    private void serveTablePageWithWebResourceManager(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");

        try {
            // Подключаем CSS ресурсы через WebResourceManager
            webResourceManager.requireResource("com.noname.plugin:mail-table-resources");

            // Читаем VM файл как обычный текст
            InputStream vmStream = getClass().getClassLoader().getResourceAsStream("templates/mail-table-clean.vm");

            if (vmStream != null) {
                String vmContent = generateStringBuffer(vmStream).toString();

                // Простая замена Velocity переменных на актуальные значения
                String processedContent = vmContent
                        .replace("$webResourceManager.requireResource(\"com.noname.plugin:mail-table-resources\")",
                                "<!-- CSS подключен через WebResourceManager в сервлете -->")
                        .replace("$contextPath", req.getContextPath())
                        .replace("$baseUrl", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath());

                resp.getWriter().write(processedContent);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Table template not found");
            }

        } catch (Exception e) {
            log.error("Error serving table page", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing template");
        }
    }

    private StringBuilder generateStringBuffer(InputStream htmlStream) throws IOException {
        StringBuilder html = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append(System.lineSeparator());
            }
        }
        return html;
    }

    private String generateNavigationPage() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <title>Mail Items Viewer</title>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }" +
                "        .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "        h1 { color: #333; text-align: center; margin-bottom: 30px; }" +
                "        .nav-button { display: block; width: 100%; padding: 15px; margin: 15px 0; background-color: #0052cc; color: white; text-decoration: none; text-align: center; border-radius: 5px; font-size: 16px; transition: background-color 0.3s; }" +
                "        .nav-button:hover { background-color: #003d99; }" +
                "        .description { color: #666; margin-bottom: 10px; font-size: 14px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <h1>Mail Items Viewer</h1>" +
                "        <p class='description'>Выберите, как вы хотите просмотреть данные о письмах:</p>" +
                "        <a href='data' class='nav-button'>📊 Данные (JSON)</a>" +
                "        <p class='description'>Получить все письма в формате JSON для API или программного использования</p>" +
                "        <a href='table' class='nav-button'>📋 Таблица (HTML)</a>" +
                "        <p class='description'>Просмотреть письма в удобной табличной форме в браузере</p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    private void createMailItem(String from, List<String> to, String subject, String body) {
        mailItemService.createMailItem(new MailItem(from, to, subject, body));
    }

    private void serveCssFile(HttpServletResponse resp, String cssPath) throws IOException {
        resp.setContentType("text/css; charset=UTF-8");
        InputStream cssStream = getClass().getClassLoader().getResourceAsStream(cssPath);

        if (cssStream != null) {
            StringBuilder css = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(cssStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    css.append(line).append(System.lineSeparator());
                }
            }
            resp.getWriter().write(css.toString());
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "CSS file not found");
        }
    }

    private void handleDeleteAll(HttpServletResponse resp) throws IOException {
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            boolean deleted = mailItemService.deleteAllMailItemsSafe();

            String jsonResponse = String.format(
                    "{\"success\":true,\"deleted\":%s,\"message\":\"%s\"}",
                    deleted,
                    deleted ? "All mail items deleted successfully" : "No mail items to delete"
            );
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(jsonResponse);
        } catch (Exception e) {
            String errorResponse = String.format(
                    "{\"success\":false,\"error\":\"Failed to delete mail items: %s\"}",
                    e.getMessage().replace("\"", "\\\"")
            );
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(errorResponse);
        }
    }
}
