package com.noname.plugin.ao;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * @author dl
 * @date 19.06.2025 22:52
 */
public interface ActiveObjectsProvider {
    ActiveObjects getActiveObjects();
}
