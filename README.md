# Mail Catcher — JIRA Plugin

JIRA-плагин для захвата, визуального просмотра и проверки email-объектов. Создан для двух сценариев: **предпросмотр писем** — увидеть, как письмо выглядит у получателя до реальной отправки, и **тестирование шаблонов** — проверить содержимое из Spock-тестов, запускаемых внутри JIRA через ScriptRunner.

---

## Стек

| Слой | Технологии |
|---|---|
| Платформа | Atlassian JIRA 9.12.2, OSGi, Atlassian Plugin SDK |
| Backend | Java 17, Spring (`@Component` + `@Inject`), Active Objects (ORM) |
| Frontend | Vanilla JS, CSS (без фреймворков), Velocity-шаблоны |
| HTML-парсинг | Jsoup 1.17.2 (bundled в JAR) |
| Тесты | JUnit 4/5, Mockito 5, Atlassian Wired Test Runner |

---

## Архитектура

```
servlet/
  MailViewerServlet          — маршрутизация HTTP-запросов
  handler/
    MailItemRequestHandler   — обработка запросов, формирование JSON-ответов
  renderer/
    MailItemPageRenderer     — загрузка шаблонов, отдача HTML и CSS
  util/
    TestDataInitializer      — утилита для генерации тестовых данных
service/
  MailItemService            — бизнес-логика, CRUD через Active Objects
ao/
  MailItemEntity             — AO-интерфейс, таблица MAIL_ITEM_TABLE
model/
  MailItem                   — доменная модель (наследует JIRA Email)
mapper/
  MailItemMapper             — конвертация Entity → MailItem
api/
  MailItemApiService         — публичный OSGi API для ScriptRunner / тестов
```

---

## HTTP-эндпоинты

Базовый путь: `/jira/plugins/servlet/mail-items`

| Метод | Путь | Описание |
|---|---|---|
| GET | `/` | Таблица писем (HTML) |
| GET | `/table` | Таблица писем (HTML, альтернативный путь) |
| GET | `/data` | Письма в JSON (поддерживает поиск и пагинацию) |
| POST | `/add-email` | Добавить письмо (JSON-тело) |
| POST | `/delete-all` | Удалить все письма |
| POST | `/create-test-data` | Создать 5 тестовых писем |
| DELETE | `/{uuid}` | Удалить одно письмо по ID |

POST и DELETE требуют прав **системного администратора** JIRA.

### GET `/data` — параметры поиска и пагинации

| Параметр | Тип | По умолчанию | Описание |
|---|---|---|---|
| `tag` | string (повторяемый) | — | Фильтр по тегу; AND-логика — письмо должно содержать все теги |
| `offset` | int | `0` | Индекс первого элемента страницы |
| `limit` | int | `10` | Количество элементов на странице |

Ответ:
```json
{
  "items": [...],
  "total": 42,
  "offset": 0,
  "limit": 10
}
```

### POST `/add-email` — тело запроса

```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "cc": "cc@example.com",
  "bcc": "bcc@example.com",
  "subject": "Тема письма",
  "body": "<h1>HTML-тело</h1>"
}
```

Поле `to` обязательно. Поля `cc` и `bcc` необязательны.

---

## UI

Таблица писем: `<YOUR-INSTALLATION>/plugins/servlet/mail-items/`
(также через **Администрирование → Mail Viewer**).

**Список писем** — колонки: отправитель и тема с коротким превью тела.

**Поиск** — строка ввода над таблицей. Введи текст и нажми Enter — добавится тег-фильтр. Можно добавить несколько тегов; письмо показывается только если соответствует всем (AND-логика). Теги удаляются крестиком или Backspace.

**Пагинация** — кнопки «Назад» / «Вперёд» с индикатором текущей страницы появляются при количестве писем больше 10.

**Раскрытие письма** — клик по строке раскрывает с анимацией:

```
От:       sender@example.com
Кому:     recipient@example.com
Копия:    cc@example.com
─────────────────────────────
Тема письма
─────────────────────────────
Тело письма (рендеренный HTML)

[ Показать исходник ]  [ Удалить ]
```

- **«Показать исходник»** — переключает тело в raw-режим с расстановкой отступов по тегам.
- **«Удалить»** — удаляет письмо с подтверждением (без перехода на первую страницу).

---

## Публичный API (`MailItemApiService`)

`MailItemApiService` зарегистрирован как OSGi-сервис (`@ExportAsService`) и доступен из ScriptRunner и wired-тестов через `@PluginModule`.

### Методы

```groovy
// Добавление письма — возвращает UUID для последующей проверки
api.addEmail(email)                                          // Email-объект
api.addEmail(from, to, subject, body)                       // краткая форма
api.addEmail(from, to, cc, bcc, subject, body)              // с cc/bcc

// Чтение полей по UUID
api.getEmailFrom(id)      // "sender@example.com"
api.getEmailTo(id)        // "recipient@example.com"
api.getEmailCc(id)        // "cc@example.com" или null
api.getEmailBcc(id)       // "bcc@example.com" или null
api.getEmailSubject(id)   // "Тема"
api.getEmailBodyHtml(id)  // "<p>Текст</p>"
api.getEmailBodyText(id)  // "Текст" — без HTML-тегов (через Jsoup)

// Управление коллекцией
api.getEmailCount()       // количество писем в базе
api.deleteAllEmails()     // true, если что-то удалено
api.loadTestData()        // создаёт 5 тестовых писем
```

Все `getEmail*` бросают `IllegalArgumentException`, если письмо с переданным ID не найдено.

---

## Использование в Spock-тестах (wired, ScriptRunner)

`@WithPlugin` загружает плагин, `@PluginModule` инжектирует `MailItemApiService` из OSGi-реестра.

```groovy
import com.noname.plugin.api.MailItemApiService
import com.atlassian.jira.mail.Email

@WithPlugin("com.noname.plugin.mail-catcher")
class EmailTemplateSpec extends Specification {

    @PluginModule
    MailItemApiService api

    def setup() {
        api.deleteAllEmails()
    }

    def "письмо корректно отображает приветствие"() {
        given:
        def email = new Email("user@company.com")
        email.setFrom("noreply@company.com")
        email.setSubject("Добро пожаловать!")
        email.setBody("<h1>Привет, Иван!</h1><p>Рады видеть вас.</p>")

        when:
        def id = api.addEmail(email)

        then:
        api.getEmailSubject(id)  == "Добро пожаловать!"
        api.getEmailBodyText(id) == "Привет, Иван! Рады видеть вас."
        api.getEmailFrom(id)     == "noreply@company.com"
        api.getEmailTo(id)       == "user@company.com"
    }

    def "письмо с cc и bcc сохраняет все адреса"() {
        when:
        def id = api.addEmail("from@company.com", "to@company.com",
                "cc@company.com", "bcc@company.com",
                "Уведомление", "<p>Текст</p>")

        then:
        api.getEmailCc(id)  == "cc@company.com"
        api.getEmailBcc(id) == "bcc@company.com"
    }

    def "количество писем увеличивается после добавления"() {
        when:
        api.addEmail("a@test.com", "x@test.com", "Письмо 1", "<p>Тело</p>")
        api.addEmail("b@test.com", "y@test.com", "Письмо 2", "<p>Тело</p>")

        then:
        api.getEmailCount() == 2
    }

    def "deleteAllEmails очищает все письма"() {
        given:
        api.addEmail("a@test.com", "x@test.com", "Тема", "<p>Тело</p>")

        when:
        api.deleteAllEmails()

        then:
        api.getEmailCount() == 0
    }

    def "getEmailBodyText возвращает текст без HTML-тегов"() {
        when:
        def id = api.addEmail("f@test.com", "t@test.com",
                "Тема", "<h1>Заголовок</h1><p>Абзац с <strong>текстом</strong>.</p>")

        then:
        api.getEmailBodyText(id) == "Заголовок Абзац с текстом."
    }

    def "getEmailBodyHtml возвращает тело письма с HTML-тегами"() {
        when:
        def id = api.addEmail("f@test.com", "t@test.com",
                "Тема", "<h1>Заголовок</h1><p>Абзац с <strong>текстом</strong>.</p>")

        then:
        api.getEmailBodyHtml(id) == "<h1>Заголовок</h1><p>Абзац с <strong>текстом</strong>.</p>"
    }
}
```

---

## Команды разработки

```bash
# Сборка JAR
mvn clean package

# Запустить локальную JIRA с плагином (hot reload включён)
atlas-run

# То же, но с подключением дебаггера на порту 5005
atlas-debug

# Запустить unit-тесты (без JIRA)
mvn test -Dtest="MailItemRequestHandlerTest,MailItemApiServiceTest,MailItemServiceTest"

# Запустить все тесты включая wired (требует запущенную JIRA)
mvn verify
```
---

## Установка

1. Собрать JAR: `mvn clean package`
2. В JIRA открыть **Администрирование → Manage Apps → Upload app**
3. Загрузить `target/mail-catcher-*.jar`

После установки плагин появится в разделе администрирования как **Mail Viewer**.

---

## Тесты

```
src/test/java/com/noname/plugin/
├── MyComponentUnitTest.java
├── MyComponentWiredTest.java
├── api/
│   └── MailItemApiServiceTest.java        — unit, публичный API (11 тестов)
├── service/
│   └── MailItemServiceTest.java           — unit, бизнес-логика с мок AO (22 теста)
├── servlet/handler/
│   └── MailItemRequestHandlerTest.java    — unit, HTTP-логика (18 тестов)
└── wired/
    ├── MailItemServiceWiredTest.java      — wired, CRUD через реальный AO
    └── MailItemModelWiredTest.java        — wired, модель и маппер
```

`MailItem` наследует JIRA-класс `Email`, поэтому тесты модели и маппера запускаются только внутри JIRA (wired-режим).
