package com.noname.plugin.impl;

import com.atlassian.sal.api.ApplicationProperties;
import com.noname.plugin.api.MyPluginComponent;

/**
 * Реализация {@link MyPluginComponent}.
 * Использует {@link ApplicationProperties} для получения отображаемого имени JIRA-приложения.
 */
public class MyPluginComponentImpl implements MyPluginComponent {

    private final ApplicationProperties applicationProperties;

    /**
     * @param applicationProperties свойства приложения JIRA, внедряемые Spring-контейнером
     */
    public MyPluginComponentImpl(final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * {@inheritDoc}
     * Если {@code applicationProperties} недоступен (например, в тестах), возвращает {@code "myComponent"}.
     */
    @Override
    public String getName() {
        if (null != applicationProperties) {
            return "myComponent:" + applicationProperties.getDisplayName();
        }
        return "myComponent";
    }
}
