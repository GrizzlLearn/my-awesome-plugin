package com.noname.plugin.model;

import com.atlassian.jira.mail.Email;
import lombok.*;

/**
 * Доменная модель письма плагина.
 * <p>
 * Расширяет стандартный JIRA-класс {@link Email}, добавляя уникальный идентификатор,
 * имена вложений и сырые заголовки. Поля from, to, cc, bcc, subject, body наследуются от {@link Email}.
 * <p>
 * {@code id} генерируется при создании объекта и не может быть изменён — он совпадает
 * с {@code uuid}, под которым письмо хранится в базе данных.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
public class MailItem extends Email {

    /** Уникальный идентификатор письма. Соответствует UUID записи в базе данных. */
    private String id;

    /** Имена вложений через запятую. {@code null}, если вложений нет. */
    private String attachmentsName;

    /** Полные заголовки письма в формате RFC 2822. {@code null}, если письмо создано вручную. */
    private String rawHeaders;

    /**
     * @param to адрес основного получателя
     */
    public MailItem(String to) {
        super(to);
    }

    /**
     * @param to              адрес основного получателя
     * @param attachmentsName имена вложений через запятую
     */
    public MailItem(String to, String attachmentsName) {
        super(to);
        this.attachmentsName = attachmentsName;
    }

    /**
     * @param to  адрес основного получателя
     * @param cc  адреса получателей в копии
     * @param bcc адреса получателей в скрытой копии
     */
    public MailItem(String to, String cc, String bcc) {
        super(to, cc, bcc);
    }

    /**
     * @param to              адрес основного получателя
     * @param cc              адреса получателей в копии
     * @param bcc             адреса получателей в скрытой копии
     * @param attachmentsName имена вложений через запятую
     */
    public MailItem(String to, String cc, String bcc, String attachmentsName) {
        super(to, cc, bcc);
        this.attachmentsName = attachmentsName;
    }

    /**
     * @param to              адрес основного получателя
     * @param cc              адреса получателей в копии
     * @param bcc             адреса получателей в скрытой копии
     * @param attachmentsName имена вложений через запятую
     * @param rawHeaders      сырые заголовки письма в формате RFC 2822
     */
    public MailItem(String to, String cc, String bcc, String attachmentsName, String rawHeaders) {
        super(to, cc, bcc);
        this.attachmentsName = attachmentsName;
        this.rawHeaders = rawHeaders;
    }
}
