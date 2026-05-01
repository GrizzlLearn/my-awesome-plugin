package com.noname.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * Сущность Active Objects, представляющая письмо в базе данных плагина.
 * <p>
 * {@code @Preload} — все поля загружаются одним запросом при первом обращении к объекту.
 * {@code @Table} — явно задаёт имя таблицы в БД, чтобы избежать конфликтов имён.
 * Геттеры и сеттеры реализуются фреймворком автоматически.
 */
@Preload
@Table("MAIL_ITEM_TABLE")
public interface MailItemEntity extends Entity {

    /** Уникальный идентификатор письма (UUID). Используется как бизнес-ключ вместо автоинкрементного ID. */
    @Indexed
    String getUuid();
    void setUuid(String uuid);

    /** Адрес отправителя. */
    String getFrom();
    void setFrom(String from);

    /** Адрес основного получателя (поле «Кому»). */
    String getTo();
    void setTo(String to);

    /** Адреса получателей в копии (поле «CC»). */
    String getCc();
    void setCc(String cc);

    /** Адреса получателей в скрытой копии (поле «BCC»). */
    String getBcc();
    void setBcc(String bcc);

    /** Тема письма. */
    String getSubject();
    void setSubject(String subject);

    /**
     * Тело письма в формате HTML.
     * {@code UNLIMITED} — снимает ограничение VARCHAR(255) для хранения писем произвольного размера.
     */
    @StringLength(StringLength.UNLIMITED)
    String getBody();
    void setBody(String body);

    /** Имена вложений через запятую. */
    String getAttachmentsName();
    void setAttachmentsName(String attachmentsName);

    /**
     * Сырые заголовки письма (RFC 2822).
     * {@code UNLIMITED} — аналогично полю body, заголовки могут быть большими.
     */
    @StringLength(StringLength.UNLIMITED)
    String getRawHeaders();
    void setRawHeaders(String rawHeaders);
}
