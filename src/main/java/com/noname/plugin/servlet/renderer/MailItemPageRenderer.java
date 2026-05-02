package com.noname.plugin.servlet.renderer;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
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
 * Отвечает за генерацию HTML-ответов и раздачу статических ресурсов (CSS).
 * <p>
 * Шаблоны загружаются из classpath-ресурсов плагина. Переменные, такие как {@code $contextPath},
 * подставляются вручную (без полноценного движка Velocity), чего достаточно для текущего набора шаблонов.
 * CSS-файлы раздаются напрямую из classpath, минуя стандартный механизм WebResource,
 * что позволяет использовать их без полной декорации страницы JIRA.
 */
@Component
public class MailItemPageRenderer {

    private static final Logger log = LoggerFactory.getLogger(MailItemPageRenderer.class);

    private final PageBuilderService pageBuilderService;
    private final ApplicationProperties applicationProperties;

    @Inject
    public MailItemPageRenderer(@ComponentImport PageBuilderService pageBuilderService,
                                @ComponentImport ApplicationProperties applicationProperties) {
        this.pageBuilderService = checkNotNull(pageBuilderService);
        this.applicationProperties = checkNotNull(applicationProperties);
    }

    /**
     * Отдаёт страницу с таблицей писем.
     * Регистрирует CSS через {@link PageBuilderService}, загружает шаблон {@code .vm} и подставляет переменные.
     *
     * @param req  HTTP-запрос (используется для получения contextPath и baseUrl)
     * @param resp HTTP-ответ
     * @throws IOException если запись ответа завершилась ошибкой
     */
    public void renderTablePage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(HTML_CONTENT_TYPE);

        try {
            RequiredResources requiredResources = pageBuilderService.assembler().resources();
            requiredResources.requireWebResource(MAIL_TABLE_RESOURCES);

            InputStream vmStream = getClass().getClassLoader().getResourceAsStream(TABLE_TEMPLATE_PATH);

            if (vmStream != null) {
                String vmContent = readStreamToString(vmStream);
                resp.getWriter().write(processVelocityTemplate(vmContent, req));
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
     * Раздаёт CSS-файл из classpath по URI запроса.
     *
     * @param req  HTTP-запрос (URI используется для определения нужного файла)
     * @param resp HTTP-ответ
     * @throws IOException если запись ответа завершилась ошибкой
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
                resp.getWriter().write(readStreamToString(cssStream));
            } catch (IOException e) {
                log.error("Error reading CSS file: " + cssPath, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading CSS file");
            }
        } else {
            log.warn("CSS file not found: " + cssPath);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, CSS_NOT_FOUND_MESSAGE);
        }
    }

    // ===== Вспомогательные методы =====

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

    /**
     * Выполняет простую подстановку переменных в шаблоне.
     * Полноценный движок Velocity не используется — текущего набора переменных достаточно для шаблона.
     */
    private String processVelocityTemplate(String vmContent, HttpServletRequest req) {
        return vmContent
                .replace("$webResourceManager.requireResource(\"com.noname.plugin:mail-table-resources\")",
                        "<!-- CSS included via WebResourceManager in servlet -->")
                .replace("$contextPath", req.getContextPath())
                .replace("$baseUrl", buildBaseUrl(req));
    }

    private String buildBaseUrl(HttpServletRequest req) {
        String baseUrl = applicationProperties.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        return req.getScheme() + "://" + req.getServerName() + ":" +
                req.getServerPort() + req.getContextPath();
    }

    /**
     * Сопоставляет URI запроса с путём к CSS-файлу в classpath.
     *
     * @return путь в classpath или {@code null}, если URI не соответствует ни одному CSS-файлу
     */
    private String extractCssPath(String requestURI) {
        if (requestURI.endsWith(CSS_MAIN_PATH)) {
            return CSS_MAIN_RESOURCE;
        } else if (requestURI.endsWith(CSS_TABLE_PATH)) {
            return CSS_TABLE_RESOURCE;
        }
        return null;
    }

}
