# Mail Catcher — JIRA Plugin

JIRA-плагин для захвата, визуального просмотра и проверки email-объектов. Создан для двух сценариев: **предпросмотр писем** — увидеть, как письмо выглядит у получателя до реальной отправки, и **тестирование шаблонов** — проверить содержимое из Spock-тестов, запускаемых внутри JIRA через ScriptRunner.

---

## Стек

| Слой | Технологии |
|---|---|
| Платформа | Atlassian JIRA 9.12.2, OSGi, Atlassian Plugin SDK |
| Backend | Java 11, Spring (`@Component` + `@Inject`), Active Objects (ORM) |
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
| `search` | string | — | Поиск по `from`, `to`, `subject` (без учёта регистра) |
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

Поля `cc` и `bcc` необязательны. Хотя бы одно из `to`/`cc`/`bcc` должно быть заполнено.

---

## UI

Таблица писем: `http://localhost:2990/jira/plugins/servlet/mail-items/`
(также через **Администрирование → Mail Viewer**).

**Список писем** — колонки: отправитель и тема с коротким превью тела.

**Поиск** — строка ввода над таблицей фильтрует по отправителю, получателю и теме в реальном времени (debounce 300 мс).

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

Класс экспортируется через OSGi и доступен из ScriptRunner и wired-тестов.

### Добавление письма

```groovy
import com.noname.plugin.api.MailItemApiService
import com.atlassian.jira.mail.Email

def api = new MailItemApiService()

// Через Email-объект
def email = new Email("recipient@example.com")
email.setFrom("sender@example.com")
email.setSubject("Тема")
email.setBody("<p>Текст письма</p>")
def id = api.addEmail(email)

// Краткая форма (без cc/bcc)
def id2 = api.addEmail("sender@example.com", "recipient@example.com", "Тема", "<p>Текст</p>")

// С cc/bcc
def id3 = api.addEmail("from@ex.com", "to@ex.com", "cc@ex.com", "bcc@ex.com", "Тема", "<p>Текст</p>")
```

### Проверка полей письма

```groovy
api.getEmailFrom(id)     // "sender@example.com"
api.getEmailTo(id)       // "recipient@example.com"
api.getEmailCc(id)       // "cc@example.com" или null
api.getEmailBcc(id)      // "bcc@example.com" или null
api.getEmailSubject(id)  // "Тема"
api.getEmailBodyHtml(id) // "<p>Текст письма</p>"
api.getEmailBodyText(id) // "Текст письма" — без HTML-тегов (через Jsoup)
```

### Управление коллекцией

```groovy
api.getEmailCount()    // Int — количество писем в базе
api.deleteAllEmails()  // Boolean — true, если что-то удалено
api.loadTestData()     // создаёт 5 тестовых писем
```

Все методы бросают `IllegalArgumentException`, если письмо с переданным ID не найдено.

---

## Использование в Spock-тестах (wired)

```groovy
import com.noname.plugin.api.MailItemApiService
import com.atlassian.jira.mail.Email

class EmailTemplateSpec extends Specification {

    def api = new MailItemApiService()

    def cleanup() {
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

Локальная JIRA: `http://localhost:2990/jira`  
Логи плагина: `target/jira/home/log/plugin.log`

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
