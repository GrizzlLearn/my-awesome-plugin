package com.noname.plugin.servlet;

import com.atlassian.jira.util.json.JSONException;
import com.noname.plugin.model.MailItem;
import com.noname.plugin.service.MailItemService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @Inject
    public MailViewerServlet(MailItemService mailItemService) {
        this.mailItemService = checkNotNull(mailItemService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (mailItemService.getAllMailItems().isEmpty()) {
            createMailItem("qwer", Collections.singletonList("qwer"), "qwer", "qwer");
            createMailItem("qwer1", Collections.singletonList("qwer1"), "qwer1", "qwer1");
            createMailItem("qwer2", Collections.singletonList("qwer2"), "qwer2", "qwer2");
            createMailItem("qwer3", Collections.singletonList("qwer3"), "qwer3", "qwer3");
        }

        if (req.getRequestURI().endsWith("/mail-items/")) {
            resp.setContentType("text/html; charset=UTF-8");
            InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("templates/mail-main.html");
            if (htmlStream != null) {
                StringBuilder html = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        html.append(line).append(System.lineSeparator());
                    }
                }
                resp.getWriter().write(html.toString());
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "HTML not found");
            }
        }

        if (req.getRequestURI().endsWith("/mail-items/data")) {
            resp.setContentType("application/json; charset=UTF-8");
            try {
                resp.getWriter().write(mailItemService.getAllMailItemsAsJson());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        if (req.getRequestURI().endsWith("/mail-items/page")) {
            resp.setContentType("text/html; charset=UTF-8");
            InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("templates/mail-table1.html");
            if (htmlStream != null) {
                StringBuilder html = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        html.append(line).append(System.lineSeparator());
                    }
                }
                resp.getWriter().write(html.toString());
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "HTML not found");
            }
        }

    }

    private void createMailItem(String from, List<String> to, String subject, String body) {
        mailItemService.createMailItem(new MailItem(from, to, subject, body));
    }
}
