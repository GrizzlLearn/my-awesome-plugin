package com.noname.plugin.ao;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Стандартная реализация {@link ActiveObjectsProvider}.
 * Хранит ссылку на {@link ActiveObjects}, полученную через внедрение зависимостей Spring.
 */
public class ActiveObjectsProviderImpl implements ActiveObjectsProvider {

    private final ActiveObjects ao;

    /**
     * @param ao экземпляр ActiveObjects, внедряемый Spring-контейнером
     */
    public ActiveObjectsProviderImpl(ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public ActiveObjects getActiveObjects() {
        return ao;
    }
}
