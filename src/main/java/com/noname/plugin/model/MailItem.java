package com.noname.plugin.model;

import com.atlassian.jira.mail.Email;
import lombok.*;

/**
 * Расширенная модель email для плагина
 * Наследуется от стандартной JIRA Email модели
 *
 * @author dl
 * @date 24.06.2025 22:02
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class MailItem extends Email {

    /**
     * Уникальный идентификатор письма
     */
    private final String id = java.util.UUID.randomUUID().toString();

    /**
     * Имена вложений (дополнительное поле)
     */
    private String attachmentsName;

    /**
     * Конструктор с обязательным получателем
     */
    public MailItem(String to) {
        super(to);
    }

    /**
     * Конструктор с получателем и дополнительными полями
     */
    public MailItem(String to, String attachmentsName) {
        super(to);
        this.attachmentsName = attachmentsName;
    }

    /**
     * Конструктор с полным набором адресов
     */
    public MailItem(String to, String cc, String bcc) {
        super(to, cc, bcc);
    }

    /**
     * Конструктор с полным набором адресов и дополнительными полями
     */
    public MailItem(String to, String cc, String bcc, String attachmentsName) {
        super(to, cc, bcc);
        this.attachmentsName = attachmentsName;
    }

    // Все остальные свойства (from, to, cc, bcc, subject, body)
    // автоматически наследуются от Email класса!
}
