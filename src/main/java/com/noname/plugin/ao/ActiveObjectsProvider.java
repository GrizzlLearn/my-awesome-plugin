package com.noname.plugin.ao;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Интерфейс провайдера доступа к ActiveObjects.
 * Позволяет передавать зависимость на AO через конструктор без прямого использования {@code @ComponentImport}.
 */
public interface ActiveObjectsProvider {

    /**
     * Возвращает экземпляр ActiveObjects для работы с базой данных плагина.
     *
     * @return экземпляр {@link ActiveObjects}
     */
    ActiveObjects getActiveObjects();
}
