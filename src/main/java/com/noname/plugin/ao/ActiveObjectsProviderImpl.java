package com.noname.plugin.ao;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * @author dl
 * @date 19.06.2025 22:54
 */

public class ActiveObjectsProviderImpl implements ActiveObjectsProvider {
    private final ActiveObjects ao;

    public ActiveObjectsProviderImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public ActiveObjects getActiveObjects() {
        return ao;
    }
}
