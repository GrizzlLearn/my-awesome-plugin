package com.noname.plugin.api;

/**
 * Публичный API компонента плагина.
 * Экспортируется через OSGi и может использоваться другими плагинами JIRA.
 */
public interface MyPluginComponent {

    /**
     * Возвращает отображаемое имя компонента с именем приложения JIRA.
     *
     * @return строка вида {@code "myComponent:<DisplayName>"}
     */
    String getName();
}
