package com.noname.plugin.mapper;

import com.noname.plugin.ao.MailItemEntity;
import com.noname.plugin.model.MailItem;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author dl
 * @date 24.06.2025 22:19
 */
public class MailItemMapper {
    public static MailItem toDtoFull(MailItemEntity entity) {
        return MailItem.builder()
                .id(entity.getUuid())
                .from(entity.getFrom())
                .to(Arrays.asList(entity.getTo().split(",")))
                .cc(Arrays.asList(entity.getCc().split(",")))
                .bcc(Arrays.asList(entity.getBcc().split(",")))
                .subject(entity.getSubject())
                .body(entity.getBody())
                .attachmentsName(entity.getAttachmentsName())
                .build();
    }

    public static MailItem toDtoMinimal(MailItemEntity entity) {
        return MailItem.builder()
                .from(entity.getFrom())
                .to(Arrays.asList(entity.getTo().split(",")))
                .subject(entity.getSubject())
                .body(entity.getBody())
                .build();
    }

    public static void updateEntity(MailItemEntity entity, MailItem dto) {
        entity.setUuid(dto.getId());
        entity.setFrom(dto.getFrom());
        entity.setTo(String.join(",", dto.getTo()));
        entity.setCc(dto.getCc() != null ? String.join(",", dto.getCc()) : "");
        entity.setBcc(dto.getBcc() != null ? String.join(",", dto.getBcc()) : "");
        entity.setSubject(dto.getSubject());
        entity.setBody(dto.getBody());
        entity.setAttachmentsName(dto.getAttachmentsName());
    }
}
