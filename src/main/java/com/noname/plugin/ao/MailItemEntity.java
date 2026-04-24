package com.noname.plugin.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * @author dl
 * @date 24.06.2025 22:11
 */

@Preload
@Table("MAIL_ITEM_TABLE")
public interface MailItemEntity extends Entity {
    String getUuid();
    void setUuid(String uuid);

    String getFrom();
    void setFrom(String from);

    String getTo();
    void setTo(String to);

    String getCc();
    void setCc(String cc);

    String getBcc();
    void setBcc(String bcc);

    String getSubject();
    void setSubject(String subject);

    @StringLength(StringLength.UNLIMITED)
    String getBody();
    void setBody(String body);

    String getAttachmentsName();
    void setAttachmentsName(String attachmentsName);

    @StringLength(StringLength.UNLIMITED)
    String getRawHeaders();
    void setRawHeaders(String rawHeaders);
}
