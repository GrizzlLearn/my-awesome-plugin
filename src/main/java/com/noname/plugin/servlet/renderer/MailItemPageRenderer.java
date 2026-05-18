package com.noname.plugin.servlet.renderer;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
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
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.constants.MailViewerConstants.*;

/**
 * Отвечает за генерацию HTML-ответов и раздачу статических ресурсов (CSS).
 * <p>
 * Шаблоны рендерятся через {@link TemplateRenderer} (Velocity-движок Atlassian).
 * CSS-файлы раздаются напрямую из classpath, минуя стандартный механизм WebResource,
 * что позволяет использовать их без полной декорации страницы JIRA.
 */
@Component
public class MailItemPageRenderer {

    private static final Logger log = LoggerFactory.getLogger(MailItemPageRenderer.class);

    private final PageBuilderService pageBuilderService;
    private final ApplicationProperties applicationProperties;
    private final TemplateRenderer templateRenderer;

    @Inject
    public MailItemPageRenderer(@ComponentImport PageBuilderService pageBuilderService,
                                @ComponentImport ApplicationProperties applicationProperties,
                                @ComponentImport TemplateRenderer templateRenderer) {
        this.pageBuilderService = checkNotNull(pageBuilderService);
        this.applicationProperties = checkNotNull(applicationProperties);
        this.templateRenderer = checkNotNull(templateRenderer);
    }

    /**
     * Отдаёт страницу с таблицей писем.
     * Регистрирует CSS через {@link PageBuilderService} и рендерит шаблон через {@link TemplateRenderer}.
     *
     * @param req  HTTP-запрос (используется для получения contextPath и baseUrl)
     * @param resp HTTP-ответ
     * @throws IOException если запись ответа завершилась ошибкой
     */
    public void renderTablePage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(HTML_CONTENT_TYPE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            RequiredResources requiredResources = pageBuilderService.assembler().resources();
            requiredResources.requireWebResource(MAIL_TABLE_RESOURCES);

            Map<String, Object> context = new HashMap<>();
            context.put("contextPath", req.getContextPath());
            context.put("baseUrl", buildBaseUrl(req));

            templateRenderer.render(TABLE_TEMPLATE_PATH, context, resp.getWriter());
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

        try (InputStream cssStream = getClass().getClassLoader().getResourceAsStream(cssPath)) {
            if (cssStream != null) {
                resp.getWriter().write(readStreamToString(cssStream));
            } else {
                log.warn("CSS file not found: {}", cssPath);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, CSS_NOT_FOUND_MESSAGE);
            }
        } catch (IOException e) {
            log.error("Error reading CSS file: {}", cssPath, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading CSS file");
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
