package com.noname.plugin.mapper;

import com.noname.plugin.ao.MailItemEntity;
import com.noname.plugin.model.MailItem;

/**
 * Маппер между {@link MailItemEntity} (слой БД) и {@link MailItem} (доменная модель).
 * Все методы статические — класс не хранит состояния.
 */
public class MailItemMapper {

    /**
     * Преобразует сущность в полную доменную модель со всеми полями, включая вложения.
     * <p>
     * Если все поля получателя (to, cc, bcc) пустые — подставляет заглушку {@code unknown@example.com},
     * так как конструктор {@link MailItem} требует хотя бы одного получателя.
     *
     * @param entity сущность из базы данных
     * @return {@link MailItem} со всеми заполненными полями
     */
    public static MailItem toDtoFull(MailItemEntity entity) {
        String to = entity.getTo();
        String cc = entity.getCc();
        String bcc = entity.getBcc();

        if ((to == null || to.isEmpty()) && (cc == null || cc.isEmpty()) && (bcc == null || bcc.isEmpty())) {
            to = "unknown@example.com";
        }

        MailItem mailItem = new MailItem(to, cc, bcc, entity.getAttachmentsName());
        mailItem.setFrom(entity.getFrom());
        mailItem.setSubject(entity.getSubject());
        mailItem.setBody(entity.getBody());
        return mailItem;
    }

}
