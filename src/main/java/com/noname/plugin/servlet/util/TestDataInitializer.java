package com.noname.plugin.servlet.util;

import com.noname.plugin.service.MailItemService;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for initializing test data when needed.
 * Separates test data creation logic from servlet handling.
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
     * Initializes test data if no mail items exist
     * @return true if test data was created, false if data already existed
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
     * Forces creation of test data regardless of existing data
     * @return true if test data was created successfully
     */
    public boolean forceCreateTestData() {
        log.info("Force creating test data");
        return mailItemService.loadTestData();
    }

    /**
     * Checks if mail items collection is empty
     * @return true if no mail items exist
     */
    public boolean isDataEmpty() {
        return mailItemService.getAllMailItems().isEmpty();
    }

    /**
     * Gets current mail items count
     * @return number of existing mail items
     */
    public int getMailItemsCount() {
        return mailItemService.getAllMailItems().size();
    }
}
