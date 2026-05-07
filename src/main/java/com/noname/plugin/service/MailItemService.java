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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для операций с письмами: создание, чтение, удаление и загрузка тестовых данных.
 * Единственная точка доступа к {@code MAIL_ITEM_TABLE} через Active Objects.
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
     * @param email сохраняемое письмо
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
     * {@code body}, {@code attachmentsName}. Поле {@code to} обязательно — требование конструктора {@code Email}.
     *
     * @param json JSON-объект с полями письма
     * @return UUID созданной записи
     * @throws JSONException            если JSON некорректен
     * @throws IllegalArgumentException если поле {@code to} пустое или отсутствует
     */
    public String createMailItemFromJson(JSONObject json) throws JSONException {
        String to = json.optString("to", null);
        String cc = json.optString("cc", null);
        String bcc = json.optString("bcc", null);

        if (to == null || to.isEmpty()) {
            throw new IllegalArgumentException("Field 'to' is required");
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
     * Возвращает количество писем в базе данных без загрузки всех записей в память.
     *
     * @return количество записей
     */
    public int countMailItems() {
        return ao.count(MailItemEntity.class);
    }

    /**
     * Возвращает страницу писем в виде JSON-строки с поддержкой поиска, пагинации и курсора.
     * Каждый тег должен совпадать хотя бы с одним полем письма (from, to, subject, body) —
     * семантика AND: письмо включается только если соответствует всем переданным тегам.
     * Если {@code sinceId > 0}, возвращаются только записи с ID > sinceId (режим обновления).
     *
     * @param tags    массив тегов поиска или {@code null} для возврата всех писем
     * @param offset  нулевой индекс первого элемента страницы
     * @param limit   максимальное количество элементов на странице (0 — вернуть все)
     * @param sinceId курсор: вернуть только записи с ID строго больше этого значения; 0 — без фильтра
     * @return JSON-объект с полями {@code items}, {@code total}, {@code offset}, {@code limit}, {@code maxId}
     * @throws JSONException если построение JSON-ответа завершилось ошибкой
     */
    public String getAllMailItemsAsJson(String[] tags, int offset, int limit, int sinceId) throws JSONException {
        // Строим WHERE-условие на уровне SQL: каждый тег AND-группа по четырём полям
        StringBuilder where = new StringBuilder();
        List<String> params = new ArrayList<>();

        if (tags != null) {
            for (String tag : tags) {
                if (tag == null || tag.trim().isEmpty()) continue;
                String t = "%" + tag.toLowerCase().trim() + "%";
                if (where.length() > 0) where.append(" AND ");
                where.append("(LOWER(\"FROM\") LIKE ? OR LOWER(\"TO\") LIKE ? OR LOWER(\"SUBJECT\") LIKE ? OR LOWER(\"BODY\") LIKE ?)");
                params.add(t);
                params.add(t);
                params.add(t);
                params.add(t);
            }
        }

        // Курсор: фильтруем записи новее lastMaxId
        if (sinceId > 0) {
            if (where.length() > 0) where.append(" AND ");
            where.append("ID > ?");
            params.add(String.valueOf(sinceId));
        }

        int safeOffset = Math.max(0, offset);
        int safeLimit = limit;

        Query pageQuery = Query.select().offset(safeOffset);
        Query countQuery = Query.select();

        if (where.length() > 0) {
            Object[] args = params.toArray();
            pageQuery = pageQuery.where(where.toString(), args);
            countQuery = countQuery.where(where.toString(), args);
        }

        int total = ao.count(MailItemEntity.class, countQuery);

        List<MailItemEntity> page;
        if (total == 0) {
            page = List.of();
        } else {
            int effectiveLimit = (safeLimit <= 0) ? total : safeLimit;
            pageQuery = pageQuery.limit(effectiveLimit);
            page = Arrays.asList(ao.find(MailItemEntity.class, pageQuery));
        }

        JSONArray array = new JSONArray();
        int maxId = sinceId; // если страница пустая — курсор не регрессирует
        for (MailItemEntity entity : page) {
            JSONObject obj = new JSONObject();
            obj.put("id", entity.getUuid());
            obj.put("dbId", entity.getID());
            obj.put("from", entity.getFrom());
            obj.put("to", entity.getTo());
            obj.put("cc", entity.getCc());
            obj.put("bcc", entity.getBcc());
            obj.put("subject", entity.getSubject());
            obj.put("body", entity.getBody());
            obj.put("attachmentsName", entity.getAttachmentsName());
            // Обновляем максимальный ID среди элементов текущей страницы
            if (entity.getID() > maxId) maxId = entity.getID();
            array.put(obj);
        }

        int effectiveLimitForResponse = (safeLimit <= 0) ? total : safeLimit;
        JSONObject result = new JSONObject();
        result.put("items", array);
        result.put("total", total);
        result.put("offset", offset);
        result.put("limit", effectiveLimitForResponse);
        result.put("maxId", maxId);
        return result.toString();
    }

    /**
     * Удаляет все письма из базы данных.
     *
     * @return {@code true} если удалена хотя бы одна запись; {@code false} если таблица была пустой
     * @throws RuntimeException если удаление завершилось ошибкой
     */
    public boolean deleteAllMailItemsSafe() {
        // Батчевое удаление: по 200 записей за итерацию, offset не нужен — после удаления записи сдвигаются
        try {
            boolean deletedAny = false;
            while (true) {
                MailItemEntity[] batch = ao.find(MailItemEntity.class, Query.select().limit(200));
                if (batch.length == 0) break;
                for (MailItemEntity entity : batch) {
                    ao.delete(entity);
                }
                deletedAny = true;
            }
            return deletedAny;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all mail items", e);
        }
    }

    /**
     * Удаляет письмо по UUID.
     *
     * @param uuid идентификатор письма
     * @return {@code true} если письмо найдено и удалено; {@code false} если не найдено
     */
    public boolean deleteMailItemById(String uuid) {
        MailItemEntity[] results = ao.find(MailItemEntity.class, Query.select().where("UUID = ?", uuid));
        if (results.length == 0) return false;
        ao.delete(results[0]);
        return true;
    }

    /**
     * Создаёт 5 тестовых писем с HTML-содержимым.
     * Нумерация начинается с {@code (текущее количество + 1)} во избежание конфликтов с существующими записями.
     *
     * @return {@code true} при успешном выполнении
     * @throws RuntimeException если создание записей завершилось ошибкой
     */
    public boolean loadTestData() {
        try {
            int startIndex = countMailItems() + 1;

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
            throw new RuntimeException("Failed to create test data", e);
        }
    }

}
