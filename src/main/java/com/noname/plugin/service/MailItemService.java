package com.noname.plugin.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.noname.plugin.ao.MailItemEntity;
import com.noname.plugin.mapper.MailItemMapper;
import com.noname.plugin.model.MailItem;
import net.java.ao.Query;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для работы с письмами: создание, чтение, удаление, загрузка тестовых данных.
 * Единственная точка доступа к таблице {@code MAIL_ITEM_TABLE} через Active Objects.
 */
@Component
public class MailItemService {

    @ComponentImport
    private final ActiveObjects ao;

    @Inject
    public MailItemService(ActiveObjects ao) {
        this.ao = ao;
    }

    /**
     * Сохраняет письмо в базе данных и возвращает его UUID.
     * Если передан {@link MailItem}, дополнительно сохраняет вложения и сырые заголовки.
     *
     * @param email письмо для сохранения
     * @return UUID созданной записи
     * @throws IllegalArgumentException если {@code email} равен {@code null}
     */
    public String createMailItem(Email email) {
        if (email == null) throw new IllegalArgumentException("Email cannot be null");
        String uuid = UUID.randomUUID().toString();
        MailItemEntity entity = ao.create(MailItemEntity.class);
        entity.setUuid(uuid);
        entity.setFrom(email.getFrom());
        entity.setTo(email.getTo());
        entity.setCc(email.getCc());
        entity.setBcc(email.getBcc());
        entity.setSubject(email.getSubject());
        entity.setBody(email.getBody());
        if (email instanceof MailItem) {
            MailItem mailItem = (MailItem) email;
            entity.setAttachmentsName(mailItem.getAttachmentsName());
            entity.setRawHeaders(mailItem.getRawHeaders());
        }
        entity.save();
        return uuid;
    }

    /**
     * Создаёт письмо из JSON-объекта (используется HTTP-эндпоинтом {@code /add-email}).
     * Ожидаемые поля: {@code from}, {@code to}, {@code cc}, {@code bcc}, {@code subject},
     * {@code body}, {@code attachmentsName}. Хотя бы одно из полей получателя (to/cc/bcc) обязательно.
     *
     * @param json JSON-объект с полями письма
     * @return UUID созданной записи
     * @throws JSONException            если JSON некорректен
     * @throws IllegalArgumentException если все поля получателя пусты
     */
    public String createMailItemFromJson(JSONObject json) throws JSONException {
        String to = json.optString("to", null);
        String cc = json.optString("cc", null);
        String bcc = json.optString("bcc", null);

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

    /**
     * Возвращает письмо по UUID.
     *
     * @param uuid идентификатор письма
     * @return {@link MailItem} или {@code null}, если письмо не найдено
     */
    public MailItem getMailItemById(String uuid) {
        MailItemEntity[] results = ao.find(MailItemEntity.class, Query.select().where("UUID = ?", uuid));
        if (results.length == 0) return null;
        return MailItemMapper.toDtoFull(results[0]);
    }

    /**
     * Возвращает все письма из базы данных в виде доменных объектов.
     *
     * @return список всех писем; пустой список, если записей нет
     */
    public List<MailItem> getAllMailItems() {
        return Arrays.stream(ao.find(MailItemEntity.class))
                .map(MailItemMapper::toDtoFull)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает все письма в виде JSON-строки (используется HTTP-эндпоинтом {@code /data}).
     * Читает данные напрямую из сущностей, минуя маппер, чтобы не создавать лишних объектов.
     *
     * @return JSON-массив со всеми письмами
     * @throws JSONException если сборка JSON не удалась
     */
    public String getAllMailItemsAsJson() throws JSONException {
        JSONArray array = new JSONArray();

        for (MailItemEntity entity : ao.find(MailItemEntity.class)) {
            JSONObject obj = new JSONObject();
            obj.put("id", entity.getUuid());
            obj.put("from", entity.getFrom());
            obj.put("to", entity.getTo());
            obj.put("cc", entity.getCc());
            obj.put("bcc", entity.getBcc());
            obj.put("subject", entity.getSubject());
            obj.put("body", entity.getBody());
            obj.put("attachmentsName", entity.getAttachmentsName());
            array.put(obj);
        }

        return array.toString();
    }

    /**
     * Удаляет все письма из базы данных.
     *
     * @return {@code true}, если хотя бы одна запись была удалена; {@code false}, если таблица была пуста
     * @throws RuntimeException если удаление завершилось с ошибкой
     */
    public boolean deleteAllMailItemsSafe() {
        try {
            MailItemEntity[] entities = ao.find(MailItemEntity.class);
            if (entities.length > 0) {
                ao.delete(entities);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении всех объектов email", e);
        }
    }

    /**
     * Добавляет 5 тестовых писем с HTML-содержимым.
     * Нумерация начинается с {@code (текущее_количество + 1)}, чтобы не конфликтовать с существующими записями.
     *
     * @return {@code true} при успехе
     * @throws RuntimeException если создание записей завершилось с ошибкой
     */
    public boolean loadTestData() {
        try {
            int startIndex = ao.find(MailItemEntity.class).length + 1;

            for (int i = startIndex; i < startIndex + 5; i++) {
                MailItemEntity entity = ao.create(MailItemEntity.class);
                entity.setUuid(UUID.randomUUID().toString());
                entity.setFrom("sender" + i + "@example.com");
                entity.setTo("recipient" + i + "@example.com");
                entity.setSubject("Тестовое письмо #" + i);
                entity.setBody(
                    "<h2>Тестовое письмо №" + i + "</h2>" +
                    "<p>Lorem ipsum dolor sit amet, <strong>consectetur adipiscing elit</strong>. " +
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>" +
                    "<ul>" +
                    "<li>Пункт первый — <em>важная информация</em></li>" +
                    "<li>Пункт второй — <a href=\"#\">ссылка на ресурс</a></li>" +
                    "<li>Пункт третий — обычный текст</li>" +
                    "</ul>" +
                    "<p>Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris. " +
                    "Duis aute irure dolor in <code>reprehenderit</code> in voluptate velit esse.</p>" +
                    "<blockquote>Цитата: excepteur sint occaecat cupidatat non proident.</blockquote>"
                );
                entity.save();
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании тестовых данных", e);
        }
    }
}
