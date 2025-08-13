package com.noname.plugin.mapper;

import com.noname.plugin.ao.MailItemEntity;
import com.noname.plugin.model.MailItem;


/**
 * @author dl
 * @date 24.06.2025 22:19
 */
public class MailItemMapper {
    public static MailItem toDtoFull(MailItemEntity entity) {
        MailItem mailItem = new MailItem(entity.getTo(), entity.getCc(), entity.getBcc(), 
                                         entity.getAttachmentsName(), entity.getRawHeaders());
        mailItem.setFrom(entity.getFrom());
        mailItem.setSubject(entity.getSubject());
        mailItem.setBody(entity.getBody());
        return mailItem;
    }

    public static MailItem toDtoMinimal(MailItemEntity entity) {
        MailItem mailItem = new MailItem(entity.getTo());
        mailItem.setFrom(entity.getFrom());
        mailItem.setSubject(entity.getSubject());
        mailItem.setBody(entity.getBody());
        return mailItem;
    }

    public static void updateEntity(MailItemEntity entity, MailItem dto) {
        entity.setUuid(dto.getId());
        entity.setFrom(dto.getFrom());
        entity.setTo(dto.getTo());
        entity.setCc(dto.getCc());
        entity.setBcc(dto.getBcc());
        entity.setSubject(dto.getSubject());
        entity.setBody(dto.getBody());
        entity.setAttachmentsName(dto.getAttachmentsName());
        entity.setRawHeaders(dto.getRawHeaders());
    }
}
