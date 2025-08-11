package com.noname.plugin.servlet.renderer;

import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * Отрисовывает HTML-страницы и обслуживает статические ресурсы для просмотрщика почтовых элементов.
 * Обрабатывает обработку шаблонов и обслуживание CSS.
 * @author dl
 * @date 11.08.2025 22:35
 */
public class MailItemPageRenderer {
    private static final Logger log = Logger.getLogger(MailItemPageRenderer.class.getName());

    private final PageBuilderService pageBuilderService;

    public MailItemPageRenderer(PageBuilderService pageBuilderService) {
        this.pageBuilderService = checkNotNull(pageBuilderService);
    }

    /**
     * Отрисовывает главную страницу навигации
     */
    public void renderMainPage(HttpServletResponse resp) throws IOException {
        resp.setContentType(HTML_CONTENT_TYPE);

        // Try to load template from resources first
        InputStream htmlStream = getClass().getClassLoader().getResourceAsStream(MAIN_TEMPLATE_PATH);

        if (htmlStream != null) {
            try {
                String content = readStreamToString(htmlStream);
                resp.getWriter().write(content);
            } catch (IOException e) {
                log.warn("Error reading main template, falling back to generated HTML", e);
                resp.getWriter().write(generateNavigationHtml());
            }
        } else {
            log.debug("Main template not found, using generated HTML");
            resp.getWriter().write(generateNavigationHtml());
        }
    }

    /**
     * Отрисовывает страницу таблицы с интеграцией WebResourceManager
     */
    public void renderTablePage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(HTML_CONTENT_TYPE);

        try {
            // Include CSS resources through PageBuilderService
            RequiredResources requiredResources = pageBuilderService.assembler().resources();
            requiredResources.requireWebResource(MAIL_TABLE_RESOURCES);

            // Load and process template
            InputStream vmStream = getClass().getClassLoader().getResourceAsStream(TABLE_TEMPLATE_PATH);

            if (vmStream != null) {
                String vmContent = readStreamToString(vmStream);
                String processedContent = processVelocityTemplate(vmContent, req);
                resp.getWriter().write(processedContent);
            } else {
                log.error("Table template not found: " + TABLE_TEMPLATE_PATH);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, TEMPLATE_NOT_FOUND_MESSAGE);
            }

        } catch (Exception e) {
            log.error("Error serving table page", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing template");
        }
    }

    /**
     * Обслуживает CSS-файлы из classpath
     */
    public void serveCssFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestURI = req.getRequestURI();
        String cssPath = extractCssPath(requestURI);

        if (cssPath == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, CSS_NOT_FOUND_MESSAGE);
            return;
        }

        resp.setContentType(CSS_CONTENT_TYPE);

        InputStream cssStream = getClass().getClassLoader().getResourceAsStream(cssPath);

        if (cssStream != null) {
            try {
                String cssContent = readStreamToString(cssStream);
                resp.getWriter().write(cssContent);
            } catch (IOException e) {
                log.error("Error reading CSS file: " + cssPath, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading CSS file");
            }
        } else {
            log.warn("CSS file not found: " + cssPath);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, CSS_NOT_FOUND_MESSAGE);
        }
    }

    // Helper methods

    private String readStreamToString(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        }

        return result.toString();
    }

    private String processVelocityTemplate(String vmContent, HttpServletRequest req) {
        // Simple template variable replacement (could be enhanced with proper Velocity engine)
        return vmContent
                .replace("$webResourceManager.requireResource(\"com.noname.plugin:mail-table-resources\")",
                        "<!-- CSS подключен через WebResourceManager в сервлете -->")
                .replace("$contextPath", req.getContextPath())
                .replace("$baseUrl", buildBaseUrl(req));
    }

    private String buildBaseUrl(HttpServletRequest req) {
        return req.getScheme() + "://" + req.getServerName() + ":" +
                req.getServerPort() + req.getContextPath();
    }

    private String extractCssPath(String requestURI) {
        if (requestURI.endsWith(CSS_MAIN_PATH)) {
            return CSS_MAIN_RESOURCE;
        } else if (requestURI.endsWith(CSS_TABLE_PATH)) {
            return CSS_TABLE_RESOURCE;
        }
        return null;
    }

    private String generateNavigationHtml() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <title>Mail Items Viewer</title>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <style>" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }" +
                "        .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "        h1 { color: #333; text-align: center; margin-bottom: 30px; font-size: 28px; }" +
                "        .nav-button { display: block; width: 100%; padding: 15px; margin: 15px 0; background-color: #0052cc; color: white; text-decoration: none; text-align: center; border-radius: 5px; font-size: 16px; transition: all 0.3s; border: none; cursor: pointer; }" +
                "        .nav-button:hover { background-color: #003d99; transform: translateY(-1px); }" +
                "        .description { color: #666; margin-bottom: 10px; font-size: 14px; line-height: 1.5; }" +
                "        .icon { margin-right: 8px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <h1>📧 Mail Items Viewer</h1>" +
                "        <p class='description'>Выберите, как вы хотите просмотреть данные о письмах:</p>" +
                "        " +
                "        <a href='data' class='nav-button'>" +
                "            <span class='icon'>📊</span>Данные (JSON)" +
                "        </a>" +
                "        <p class='description'>Получить все письма в формате JSON для API или программного использования</p>" +
                "        " +
                "        <a href='table' class='nav-button'>" +
                "            <span class='icon'>📋</span>Таблица (HTML)" +
                "        </a>" +
                "        <p class='description'>Просмотреть письма в удобной табличной форме в браузере</p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}
