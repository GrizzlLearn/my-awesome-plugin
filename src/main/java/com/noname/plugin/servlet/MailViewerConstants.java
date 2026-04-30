package com.noname.plugin.servlet;

/**
 * Константы для сервлета Mail Viewer: URL-паттерны, пути к ресурсам, ключи WebResource, тексты ответов.
 * Централизует все «магические строки» плагина в одном месте.
 */
public final class MailViewerConstants {

    // --- URL-паттерны (GET) ---

    /** Базовый путь без слеша — используется для редиректа на {@link #MAIL_ITEMS_ROOT}. */
    public static final String MAIL_ITEMS_BASE = "/mail-items";

    /** Корневой путь просмотрщика, отдаёт HTML-таблицу. */
    public static final String MAIL_ITEMS_ROOT = "/mail-items/";

    /** Путь JSON-эндпоинта для получения всех писем. */
    public static final String MAIL_ITEMS_DATA = "/mail-items/data";

    /** Альтернативный путь для отображения таблицы писем. */
    public static final String MAIL_ITEMS_TABLE = "/mail-items/table";

    // --- POST-эндпоинты (pathInfo) ---

    /** Эндпоинт удаления всех писем. Требует прав системного администратора. */
    public static final String DELETE_ALL_ENDPOINT = "/delete-all";

    /** Эндпоинт принудительного создания тестовых писем. */
    public static final String CREATE_TEST_DATA_ENDPOINT = "/create-test-data";

    /** Эндпоинт добавления письма через JSON-тело запроса. */
    public static final String ADD_EMAIL_ENDPOINT = "/add-email";

    // --- Пути к CSS-файлам (в URL) ---

    public static final String CSS_MAIN_PATH = "/css/mail-main.css";
    public static final String CSS_TABLE_PATH = "/css/mail-table.css";

    // --- Пути к ресурсам в classpath ---

    public static final String TABLE_TEMPLATE_PATH = "templates/mail-table-clean.vm";
    public static final String CSS_MAIN_RESOURCE  = "css/mail-main.css";
    public static final String CSS_TABLE_RESOURCE = "css/mail-table.css";

    // --- Ключи WebResource ---

    /** Ключ ресурсного модуля плагина для подключения CSS через PageBuilderService. */
    public static final String MAIL_TABLE_RESOURCES = "com.noname.plugin:mail-table-resources";

    // --- Content-Type ---

    public static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";
    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    public static final String CSS_CONTENT_TYPE  = "text/css; charset=UTF-8";

    // --- Сообщения об ошибках ---

    public static final String ACCESS_DENIED_MESSAGE      = "Access denied: Admin rights required";
    public static final String ENDPOINT_NOT_FOUND_MESSAGE = "Endpoint not found";
    public static final String INTERNAL_ERROR_MESSAGE     = "Internal server error";
    public static final String TEMPLATE_NOT_FOUND_MESSAGE = "Template not found";
    public static final String CSS_NOT_FOUND_MESSAGE      = "CSS file not found";

    // --- Сообщения об успехе ---

    public static final String DELETE_SUCCESS_MESSAGE      = "All mail items deleted successfully";
    public static final String DELETE_NOTHING_MESSAGE      = "No mail items to delete";
    public static final String DELETE_BY_ID_SUCCESS_MESSAGE = "Mail item deleted successfully";
    public static final String TEST_DATA_SUCCESS_MESSAGE   = "Test data created successfully";
    public static final String TEST_DATA_ERROR_MESSAGE     = "Failed to create test data";

    // --- Сообщения об отсутствии данных ---

    public static final String EMAIL_NOT_FOUND_MESSAGE = "Mail item not found";

    // --- JIRA URL ---

    public static final String JIRA_LOGIN_URL = "/jira/login.jsp";

    private MailViewerConstants() {}
}
