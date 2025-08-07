package com.noname.plugin.servlet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author dl
 * @date 23.06.2025 12:24
 */
public class TestLoggerServlet extends HttpServlet {
    private static final org.apache.log4j.Logger slf4l = Logger.getLogger(TestLoggerServlet.class);

    static {
        PropertyConfigurator.configure("/Users/dl/projects/work/my-awesome-plugin/src/main/resources/log4j.properties"); // Явная загрузка
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        File logFile = new File("/Users/dl/projects/work/my-awesome-plugin/target/jira/home/log/plugin.log");
        if (!logFile.exists()) {
            slf4l.info("slf4l Log file does not exist, trying to create it.");
            logFile.createNewFile(); // Попытается создать, если директория ок
        }
        slf4l.debug("slf4l Debug message");
        slf4l.info("slf4l Info message");
        slf4l.warn("slf4l Warn message");
        slf4l.error("slf4l Error message");
        resp.getWriter().write("Check plugin.log and console for details");
    }
}
