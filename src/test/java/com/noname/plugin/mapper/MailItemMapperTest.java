package com.noname.plugin.mapper;

import com.atlassian.jira.JiraApplicationContext;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.noname.plugin.ao.MailItemEntity;
import com.noname.plugin.model.MailItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailItemMapperTest {

    /**
     * Запускает маппер внутри блока с заглушками ComponentAccessor,
     * чтобы конструктор Email(to, cc, bcc) не падал при отсутствии JIRA-контейнера.
     */
    private static void withMockedJiraContext(Runnable block) {
        ApplicationProperties props = mock(ApplicationProperties.class);
        when(props.getMailEncoding()).thenReturn("UTF-8");
        when(props.getOption(anyString())).thenReturn(false);

        JiraApplicationContext ctx = mock(JiraApplicationContext.class);
        when(ctx.getFingerPrint()).thenReturn("test-fingerprint");

        try (MockedStatic<ComponentAccessor> ca = mockStatic(ComponentAccessor.class)) {
            ca.when(ComponentAccessor::getApplicationProperties).thenReturn(props);
            ca.when(() -> ComponentAccessor.getComponentOfType(JiraApplicationContext.class)).thenReturn(ctx);
            block.run();
        }
    }

    @Test
    @DisplayName("toDtoFull: все поля получателя пустые — подставляется unknown@example.com")
    void toDtoFull_allRecipientFieldsEmpty_fallsBackToUnknown() {
        MailItemEntity entity = mock(MailItemEntity.class);
        when(entity.getTo()).thenReturn(null);
        when(entity.getCc()).thenReturn(null);
        when(entity.getBcc()).thenReturn(null);
        when(entity.getUuid()).thenReturn("uuid-1");
        when(entity.getFrom()).thenReturn("from@test.com");
        when(entity.getSubject()).thenReturn("Subject");
        when(entity.getBody()).thenReturn("<p>Body</p>");
        when(entity.getAttachmentsName()).thenReturn(null);
        when(entity.getRawHeaders()).thenReturn(null);

        withMockedJiraContext(() -> {
            MailItem result = MailItemMapper.toDtoFull(entity);
            assertEquals("unknown@example.com", result.getTo());
        });
    }

    @Test
    @DisplayName("toDtoFull: все поля маппируются корректно")
    void toDtoFull_normalEntity_mapsAllFields() {
        MailItemEntity entity = mock(MailItemEntity.class);
        when(entity.getTo()).thenReturn("to@test.com");
        when(entity.getCc()).thenReturn("cc@test.com");
        when(entity.getBcc()).thenReturn("bcc@test.com");
        when(entity.getUuid()).thenReturn("uuid-2");
        when(entity.getFrom()).thenReturn("from@test.com");
        when(entity.getSubject()).thenReturn("Hello");
        when(entity.getBody()).thenReturn("<p>Hi</p>");
        when(entity.getAttachmentsName()).thenReturn("file.pdf");
        when(entity.getRawHeaders()).thenReturn("From: from@test.com");
        when(entity.getCreatedAt()).thenReturn(1716000000000L);

        withMockedJiraContext(() -> {
            MailItem result = MailItemMapper.toDtoFull(entity);
            assertEquals("uuid-2", result.getId());
            assertEquals("from@test.com", result.getFrom());
            assertEquals("to@test.com", result.getTo());
            assertEquals("cc@test.com", result.getCc());
            assertEquals("bcc@test.com", result.getBcc());
            assertEquals("Hello", result.getSubject());
            assertEquals("<p>Hi</p>", result.getBody());
            assertEquals("file.pdf", result.getAttachmentsName());
            assertEquals("From: from@test.com", result.getRawHeaders());
            assertEquals(1716000000000L, result.getCreatedAt());
        });
    }
}
