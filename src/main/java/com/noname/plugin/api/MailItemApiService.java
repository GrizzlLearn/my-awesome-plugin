package com.noname.plugin.api;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mail.Email;
import com.noname.plugin.service.MailItemService;
import com.noname.plugin.model.MailItem;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.json.JSONException;

/**
 * Публичный API сервис для работы с MailItem из внешних скриптов (например, ScriptRunner)
 * 
 * Пример использования в ScriptRunner:
 * 
 * import com.noname.plugin.api.MailItemApiService
 * import com.atlassian.jira.mail.Email
 * 
 * def apiService = new MailItemApiService()
 * 
 * // Способ 1: Напрямую через Email объект
 * def email = new Email("recipient@example.com")
 * email.setFrom("sender@example.com")
 * email.setSubject("Test Email")
 * email.setBody("This is a test email")
 * def emailId = apiService.addEmail(email)
 * 
 * // Способ 2: Через простые параметры (для совместимости)
 * def emailId2 = apiService.addEmail("sender@example.com", "recipient@example.com", "Subject", "Body")
 * 
 * @author dl
 * @date 13.08.2025
 */
public class MailItemApiService {
    
    private final MailItemService mailItemService;
    
    public MailItemApiService() {
        this.mailItemService = ComponentAccessor.getOSGiComponentInstanceOfType(MailItemService.class);
    }
    
    /**
     * Добавляет Email объект в систему (основной метод)
     * 
     * @param email объект Email для сохранения
     * @return ID созданного MailItem
     * @throws IllegalArgumentException если email null
     */
    public String addEmail(Email email) {
        if (email == null) {
            throw new IllegalArgumentException("Email object cannot be null");
        }
        
        MailItem createdItem = mailItemService.createMailItemFromEmail(email);
        return createdItem.getId();
    }
    
    /**
     * Добавляет email в систему с базовыми параметрами (метод для совместимости)
     * 
     * @param from отправитель
     * @param to получатель
     * @param subject тема
     * @param body содержание
     * @return ID созданного MailItem
     */
    public String addEmail(String from, String to, String subject, String body) {
        Email email = new Email(to);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        
        return addEmail(email);
    }
    
    /**
     * Добавляет email в систему с полным набором параметров (метод для совместимости)
     * 
     * @param from отправитель
     * @param to получатель
     * @param cc копия
     * @param bcc скрытая копия
     * @param subject тема
     * @param body содержание
     * @return ID созданного MailItem
     */
    public String addEmail(String from, String to, String cc, String bcc, String subject, String body) {
        Email email = new Email(to, cc, bcc);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        
        return addEmail(email);
    }
    
    /**
     * Получает общее количество email объектов в системе
     * 
     * @return количество MailItem в базе данных
     */
    public int getEmailCount() {
        return mailItemService.getAllMailItems().size();
    }
    
    /**
     * Удаляет все email объекты из системы
     * 
     * @return true если объекты были удалены, false если удалять было нечего
     */
    public boolean deleteAllEmails() {
        return mailItemService.deleteAllMailItemsSafe();
    }
}