package com.noname.plugin.servlet.util;

import com.noname.plugin.service.MailItemService;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Вспомогательный класс для инициализации тестовых данных при необходимости.
 * Отделяет логику создания тестовых данных от обработки сервлетов.
 * @author dl
 * @date 11.08.2025 22:35
 */
public class TestDataInitializer {
    private static final Logger log = Logger.getLogger(TestDataInitializer.class.getName());

    private final MailItemService mailItemService;

    public TestDataInitializer(MailItemService mailItemService) {
        this.mailItemService = checkNotNull(mailItemService);
    }

    /**
     * Инициализирует тестовые данные, если почтовые элементы отсутствуют
     * @return true, если тестовые данные были созданы, false, если данные уже существовали
     */
    public boolean initializeIfEmpty() {
        if (mailItemService.getAllMailItems().isEmpty()) {
            log.info("No mail items found, creating initial test data");
            return mailItemService.loadTestData();
        }

        log.debug("Mail items already exist, skipping test data initialization");
        return false;
    }

    /**
     * Принудительно создаёт тестовые данные независимо от существующих данных
     * @return true, если тестовые данные были успешно созданы
     */
    public boolean forceCreateTestData() {
        log.info("Force creating test data");
        return mailItemService.loadTestData();
    }

    /**
     * Проверяет, пуста ли коллекция почтовых элементов
     * @return true, если почтовые элементы отсутствуют
     */
    public boolean isDataEmpty() {
        return mailItemService.getAllMailItems().isEmpty();
    }

    /**
     * Получает текущее количество почтовых элементов
     * @return количество существующих почтовых элементов
     */
    public int getMailItemsCount() {
        return mailItemService.getAllMailItems().size();
    }
}
