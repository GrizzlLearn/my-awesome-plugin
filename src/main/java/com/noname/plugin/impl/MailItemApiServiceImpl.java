package com.noname.plugin.impl;

import com.atlassian.jira.mail.Email;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.noname.plugin.api.MailItemApiService;
import com.noname.plugin.model.MailItem;
import com.noname.plugin.service.MailItemService;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Реализация {@link MailItemApiService}.
 * Делегирует все операции в {@link MailItemService}.
 */
@Component
@ExportAsService(MailItemApiService.class)
public class MailItemApiServiceImpl implements MailItemApiService {

    private final MailItemService mailItemService;

    @Inject
    public MailItemApiServiceImpl(MailItemService mailItemService) {
        this.mailItemService = mailItemService;
    }

    @Override
    public String addEmail(Email email) {
        if (email == null) {
            throw new IllegalArgumentException("Email object cannot be null");
        }
        return mailItemService.createMailItem(email);
    }

    @Override
    public String addEmail(String from, String to, String subject, String body) {
        Email email = new Email(to);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        return addEmail(email);
    }

    @Override
    public String addEmail(String from, String to, String cc, String bcc, String subject, String body) {
        Email email = new Email(to, cc, bcc);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        return addEmail(email);
    }

    @Override
    public int getEmailCount() {
        return mailItemService.countMailItems();
    }

    @Override
    public boolean deleteAllEmails() {
        return mailItemService.deleteAllMailItemsSafe();
    }

    @Override
    public void loadTestData() {
        mailItemService.loadTestData();
    }

    @Override
    public String getEmailFrom(String id) {
        return getOrThrow(id).getFrom();
    }

    @Override
    public String getEmailTo(String id) {
        return getOrThrow(id).getTo();
    }

    @Override
    public String getEmailCc(String id) {
        return getOrThrow(id).getCc();
    }

    @Override
    public String getEmailBcc(String id) {
        return getOrThrow(id).getBcc();
    }

    @Override
    public String getEmailSubject(String id) {
        return getOrThrow(id).getSubject();
    }

    @Override
    public String getEmailBodyHtml(String id) {
        return getOrThrow(id).getBody();
    }

    @Override
    public String getEmailBodyText(String id) {
        String html = getOrThrow(id).getBody();
        if (html == null) return null;
        return Jsoup.parse(html).text();
    }

    private MailItem getOrThrow(String id) {
        MailItem item = mailItemService.getMailItemById(id);
        if (item == null) throw new IllegalArgumentException("Email not found: " + id);
        return item;
    }
}
