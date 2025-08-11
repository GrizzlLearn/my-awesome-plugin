package com.noname.plugin.servlet;

/**
 * Constants and configuration for Mail Viewer Servlet functionality
 * @author dl
 * @date 11.08.2025 22:35
 */
public final class MailViewerConstants {

    // URL Patterns
    public static final String MAIL_ITEMS_BASE = "/mail-items";
    public static final String MAIL_ITEMS_ROOT = "/mail-items/";
    public static final String MAIL_ITEMS_DATA = "/mail-items/data";
    public static final String MAIL_ITEMS_TABLE = "/mail-items/table";

    // POST Endpoints
    public static final String DELETE_ALL_ENDPOINT = "/delete-all";
    public static final String CREATE_TEST_DATA_ENDPOINT = "/create-test-data";

    // CSS Paths
    public static final String CSS_MAIN_PATH = "/css/mail-main.css";
    public static final String CSS_TABLE_PATH = "/css/mail-table.css";

    // Resource Paths
    public static final String MAIN_TEMPLATE_PATH = "templates/mail-main.html";
    public static final String TABLE_TEMPLATE_PATH = "templates/mail-table-clean.vm";
    public static final String CSS_MAIN_RESOURCE = "css/mail-main.css";
    public static final String CSS_TABLE_RESOURCE = "css/mail-table.css";

    // WebResource Keys
    public static final String MAIL_TABLE_RESOURCES = "com.noname.plugin:mail-table-resources";

    // Content Types
    public static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";
    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    public static final String CSS_CONTENT_TYPE = "text/css; charset=UTF-8";

    // Error Messages
    public static final String ACCESS_DENIED_MESSAGE = "Access denied: Admin rights required";
    public static final String ENDPOINT_NOT_FOUND_MESSAGE = "Endpoint not found";
    public static final String INTERNAL_ERROR_MESSAGE = "Internal server error";
    public static final String TEMPLATE_NOT_FOUND_MESSAGE = "Template not found";
    public static final String CSS_NOT_FOUND_MESSAGE = "CSS file not found";

    // Success Messages
    public static final String DELETE_SUCCESS_MESSAGE = "All mail items deleted successfully";
    public static final String DELETE_NOTHING_MESSAGE = "No mail items to delete";
    public static final String TEST_DATA_SUCCESS_MESSAGE = "Test data created successfully";
    public static final String TEST_DATA_ERROR_MESSAGE = "Failed to create test data";

    // Jira URLs
    public static final String JIRA_LOGIN_URL = "/jira/login.jsp";

    private MailViewerConstants() {
        // Utility class - no instantiation
    }
}
