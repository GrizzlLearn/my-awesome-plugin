package com.noname.plugin.servlet.util;

import com.noname.plugin.service.MailItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Вспомогательный класс для инициализации тестовых данных при необходимости.
 * Отделяет логику создания тестовых данных от обработки сервлетов.
 */
@Component
public class TestDataInitializer {
    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final MailItemService mailItemService;

    @Inject
    public TestDataInitializer(MailItemService mailItemService) {
        this.mailItemService = checkNotNull(mailItemService);
    }

    /**
     * Принудительно создаёт тестовые данные независимо от существующих данных
     * @return true, если тестовые данные были успешно созданы
     */
    public boolean forceCreateTestData() {
        log.info("Force creating test data");
        return mailItemService.loadTestData();
    }
}
