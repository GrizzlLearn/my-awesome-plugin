package com.noname.plugin.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.noname.plugin.ao.MailItemEntity;
import com.noname.plugin.mapper.MailItemMapper;
import com.noname.plugin.model.MailItem;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dl
 * @date 24.06.2025 22:25
 */
@Component
public class MailItemService {
    @ComponentImport
    private final ActiveObjects ao;

    @Inject
    public MailItemService(ActiveObjects ao) {
        this.ao = ao;
    }

    public MailItem createMailItem(MailItem item) {
        MailItemEntity entity = ao.create(MailItemEntity.class);
        MailItemMapper.updateEntity(entity, item);
        entity.save();
        return item;
    }

    /**
     * Создает MailItem из объекта Email для внешнего API
     */
    public MailItem createMailItemFromEmail(com.atlassian.jira.mail.Email email) {
        if (email == null) {
            throw new IllegalArgumentException("Email object cannot be null");
        }
        
        MailItem mailItem = new MailItem(email.getTo(), email.getCc(), email.getBcc());
        mailItem.setFrom(email.getFrom());
        mailItem.setSubject(email.getSubject());
        mailItem.setBody(email.getBody());
        
        return createMailItem(mailItem);
    }

    /**
     * Создает MailItem из JSON объекта для REST API
     */
    public MailItem createMailItemFromJson(JSONObject json) throws JSONException {
        String to = json.optString("to", null);
        String cc = json.optString("cc", null);
        String bcc = json.optString("bcc", null);
        
        // Проверяем, что хотя бы одно поле получателя заполнено
        if ((to == null || to.isEmpty()) && (cc == null || cc.isEmpty()) && (bcc == null || bcc.isEmpty())) {
            throw new IllegalArgumentException("At least one recipient field (to, cc, bcc) must be provided");
        }
        
        MailItem mailItem = new MailItem(to, cc, bcc);
        mailItem.setFrom(json.optString("from", null));
        mailItem.setSubject(json.optString("subject", null));
        mailItem.setBody(json.optString("body", null));
        
        if (json.has("attachmentsName")) {
            mailItem.setAttachmentsName(json.getString("attachmentsName"));
        }
        
        return createMailItem(mailItem);
    }

    public List<MailItem> getAllMailItems() {
        return Arrays.stream(ao.find(MailItemEntity.class))
                .map(MailItemMapper::toDtoFull)
                .collect(Collectors.toList());
    }

    public String getAllMailItemsAsJson() throws JSONException {
        List<MailItem> items = getAllMailItems();
        JSONArray array = new JSONArray();

        for (MailItem item : items) {
            JSONObject obj = new JSONObject();
            obj.put("id", item.getId());
            obj.put("from", item.getFrom());
            obj.put("to", item.getTo());
            obj.put("cc", item.getCc());
            obj.put("bcc", item.getBcc());
            obj.put("subject", item.getSubject());
            obj.put("body", item.getBody());
            obj.put("attachmentsName", item.getAttachmentsName());
            array.put(obj);
        }

        return array.toString();
    }

    public boolean deleteAllMailItemsSafe() {
        try {
            MailItemEntity[] entities = ao.find(MailItemEntity.class);
            if (entities.length > 0 ) {
                ao.delete(entities);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении всех объектов email", e);
        }
    }

    public boolean loadTestData() {
        try {
            // Получаем текущее количество записей для продолжения инкремента
            int currentCount = getAllMailItems().size();
            int startIndex = currentCount + 1;
            
            // Создаем 5 новых записей, продолжая нумерацию
            for (int i = startIndex; i < startIndex + 5; i++) {
                String from = "sender" + i + "@example.com";
                String to = "recipient" + i + "@example.com";
                String subject = "Тестовое письмо #" + i;
                String body = "Это тестовое письмо номер " + i + ". Содержимое письма для проверки функциональности.";
                
                MailItem mailItem = new MailItem(to);
                mailItem.setFrom(from);
                mailItem.setSubject(subject);
                mailItem.setBody(body);
                createMailItem(mailItem);
            }
            
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании тестовых данных", e);
        }
    }


}
