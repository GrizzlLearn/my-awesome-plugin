package com.noname.plugin.impl;

import com.atlassian.sal.api.ApplicationProperties;
import com.noname.plugin.api.MyPluginComponent;

public class MyPluginComponentImpl implements MyPluginComponent {

    private final ApplicationProperties applicationProperties;
    
    public MyPluginComponentImpl(final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String getName() {
        if (null != applicationProperties) {
            return "myComponent:" + applicationProperties.getDisplayName();
        }

        return "myComponent";
    }
}
