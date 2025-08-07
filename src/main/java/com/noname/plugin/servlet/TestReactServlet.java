package com.noname.plugin.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TestReactServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestReactServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        log.trace("trace message");
        log.debug("debug message");
        log.info("info message");
        log.warn("warn message");
        log.error("error message");

        resp.setContentType("text/html");
        resp.getWriter().write("Hello from servlet");
    }
}
