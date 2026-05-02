package com.noname.plugin.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.noname.plugin.ao.MailItemEntity;
import net.java.ao.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailItemService — бизнес-логика и слой доступа к данным")
class MailItemServiceTest {

    @Mock private ActiveObjects ao;
    @Mock private MailItemEntity entity1;
    @Mock private MailItemEntity entity2;
    @Mock private MailItemEntity entity3;

    private MailItemService service;

    @BeforeEach
    void setUp() {
        service = new MailItemService(ao);
    }

    // ===== countMailItems =====

    @Test
    @DisplayName("countMailItems() — делегирует ao.count() без загрузки записей")
    void countMailItems_delegatesToAoCount() {
        when(ao.count(MailItemEntity.class)).thenReturn(42);

        assertEquals(42, service.countMailItems());
        verify(ao).count(MailItemEntity.class);
        verify(ao, never()).find(MailItemEntity.class);
    }

    @Test
    @DisplayName("countMailItems() — возвращает 0 для пустой базы")
    void countMailItems_emptyDb_returnsZero() {
        when(ao.count(MailItemEntity.class)).thenReturn(0);
        assertEquals(0, service.countMailItems());
    }

    // ===== createMailItem =====

    @Test
    @DisplayName("createMailItem(null) — бросает IllegalArgumentException")
    void createMailItem_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.createMailItem(null));
        verifyNoInteractions(ao);
    }

    @Test
    @DisplayName("createMailItem(Email) — сохраняет запись и возвращает непустой UUID")
    void createMailItem_email_savesEntityAndReturnsUuid() {
        Email email = mock(Email.class);
        when(email.getFrom()).thenReturn("from@test.com");
        when(email.getTo()).thenReturn("to@test.com");
        when(email.getCc()).thenReturn(null);
        when(email.getBcc()).thenReturn(null);
        when(email.getSubject()).thenReturn("Тема");
        when(email.getBody()).thenReturn("<p>Текст</p>");
        when(ao.create(MailItemEntity.class)).thenReturn(entity1);

        String uuid = service.createMailItem(email);

        assertNotNull(uuid);
        assertFalse(uuid.isEmpty());
        verify(entity1).setUuid(uuid);
        verify(entity1).setFrom("from@test.com");
        verify(entity1).setTo("to@test.com");
        verify(entity1).setSubject("Тема");
        verify(entity1).setBody("<p>Текст</p>");
        verify(entity1).save();
    }

    // ===== getAllMailItemsAsJson — базовые случаи =====

    @Test
    @DisplayName("getAllMailItemsAsJson: пустая база — возвращает пустые items и total=0")
    void getAllMailItemsAsJson_emptyDb_returnsEmptyItems() throws JSONException {
        when(ao.count(MailItemEntity.class)).thenReturn(0);

        String json = service.getAllMailItemsAsJson((String[]) null, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(0, result.getJSONArray("items").length());
        assertEquals(0, result.getInt("total"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: без тегов — возвращает все записи")
    void getAllMailItemsAsJson_noTags_returnsAll() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@test.com", "x@test.com", "Тема 1", "<p>1</p>");
        stubEntity(entity2, "uuid-2", "b@test.com", "y@test.com", "Тема 2", "<p>2</p>");
        stubEntity(entity3, "uuid-3", "c@test.com", "z@test.com", "Тема 3", "<p>3</p>");
        when(ao.count(MailItemEntity.class)).thenReturn(3);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2, entity3});

        String json = service.getAllMailItemsAsJson((String[]) null, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(3, result.getInt("total"));
        assertEquals(3, result.getJSONArray("items").length());
        assertEquals("uuid-1", result.getJSONArray("items").getJSONObject(0).getString("id"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: ответ содержит поля items, total, offset, limit")
    void getAllMailItemsAsJson_responseContainsRequiredFields() throws JSONException {
        when(ao.count(MailItemEntity.class)).thenReturn(0);

        String json = service.getAllMailItemsAsJson((String[]) null, 5, 20);

        JSONObject result = new JSONObject(json);
        assertTrue(result.has("items"));
        assertTrue(result.has("total"));
        assertTrue(result.has("offset"));
        assertTrue(result.has("limit"));
        assertEquals(5, result.getInt("offset"));
        assertEquals(20, result.getInt("limit"));
    }

    // ===== getAllMailItemsAsJson — поиск =====

    @Test
    @DisplayName("getAllMailItemsAsJson: поиск по from без учёта регистра")
    void getAllMailItemsAsJson_searchByFrom_filtersCorrectly() throws JSONException {
        stubEntity(entity1, "uuid-1", "alice@test.com", "x@t.com", "Тема", null);
        stubEntity(entity2, "uuid-2", "bob@test.com",   "y@t.com", "Тема", null);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2});

        String json = service.getAllMailItemsAsJson(new String[]{"ALICE"}, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(1, result.getInt("total"));
        assertEquals("uuid-1", result.getJSONArray("items").getJSONObject(0).getString("id"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: поиск по subject")
    void getAllMailItemsAsJson_searchBySubject_filtersCorrectly() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Отчёт Q1", null);
        stubEntity(entity2, "uuid-2", "b@t.com", "y@t.com", "Привет",   null);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2});

        String json = service.getAllMailItemsAsJson(new String[]{"отчёт"}, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(1, result.getInt("total"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: поиск по body")
    void getAllMailItemsAsJson_searchByBody_filtersCorrectly() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Тема", "<p>Lorem ipsum dolor</p>");
        stubEntity(entity2, "uuid-2", "b@t.com", "y@t.com", "Тема", "<p>Привет мир</p>");
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2});

        String json = service.getAllMailItemsAsJson(new String[]{"lorem"}, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(1, result.getInt("total"));
        assertEquals("uuid-1", result.getJSONArray("items").getJSONObject(0).getString("id"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: два тега — AND-логика, оба должны совпасть")
    void getAllMailItemsAsJson_multipleTagsAndLogic() throws JSONException {
        stubEntity(entity1, "uuid-1", "alice@example.com", "x@t.com", "Тема", "<p>Lorem ipsum</p>");
        stubEntity(entity2, "uuid-2", "bob@example.com",   "y@t.com", "Тема", "<p>Привет</p>");
        stubEntity(entity3, "uuid-3", "carol@other.com",   "z@t.com", "Тема", "<p>Lorem ipsum</p>");
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2, entity3});

        // "example" matches entity1 and entity2 by from; "lorem" matches entity1 and entity3 by body
        // AND → только entity1 соответствует обоим
        String json = service.getAllMailItemsAsJson(new String[]{"example", "lorem"}, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(1, result.getInt("total"));
        assertEquals("uuid-1", result.getJSONArray("items").getJSONObject(0).getString("id"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: поиск без совпадений — возвращает пустые items")
    void getAllMailItemsAsJson_searchNoMatch_returnsEmpty() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Тема", null);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1});

        String json = service.getAllMailItemsAsJson(new String[]{"zzznomatch"}, 0, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(0, result.getInt("total"));
        assertEquals(0, result.getJSONArray("items").length());
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: null-поля сущности не вызывают NPE при поиске")
    void getAllMailItemsAsJson_searchWithNullFields_noNpe() throws JSONException {
        stubEntity(entity1, "uuid-1", null, null, null, null);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1});

        assertDoesNotThrow(() -> service.getAllMailItemsAsJson(new String[]{"test"}, 0, 10));
    }

    // ===== getAllMailItemsAsJson — пагинация =====

    @Test
    @DisplayName("getAllMailItemsAsJson: offset срезает начало списка")
    void getAllMailItemsAsJson_withOffset_skipsItems() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Тема 1", null);
        stubEntity(entity2, "uuid-2", "b@t.com", "y@t.com", "Тема 2", null);
        stubEntity(entity3, "uuid-3", "c@t.com", "z@t.com", "Тема 3", null);
        when(ao.count(MailItemEntity.class)).thenReturn(3);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity2, entity3});

        String json = service.getAllMailItemsAsJson(null, 1, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(3, result.getInt("total"));
        assertEquals(2, result.getJSONArray("items").length());
        assertEquals("uuid-2", result.getJSONArray("items").getJSONObject(0).getString("id"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: limit ограничивает количество возвращаемых элементов")
    void getAllMailItemsAsJson_withLimit_returnsCorrectCount() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Тема 1", null);
        stubEntity(entity2, "uuid-2", "b@t.com", "y@t.com", "Тема 2", null);
        stubEntity(entity3, "uuid-3", "c@t.com", "z@t.com", "Тема 3", null);
        when(ao.count(MailItemEntity.class)).thenReturn(3);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2});

        String json = service.getAllMailItemsAsJson(null, 0, 2);

        JSONObject result = new JSONObject(json);
        assertEquals(3, result.getInt("total"));
        assertEquals(2, result.getJSONArray("items").length());
        assertEquals("uuid-1", result.getJSONArray("items").getJSONObject(0).getString("id"));
        assertEquals("uuid-2", result.getJSONArray("items").getJSONObject(1).getString("id"));
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: offset за пределами total — возвращает пустые items")
    void getAllMailItemsAsJson_offsetBeyondTotal_returnsEmptyItems() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Тема", null);
        when(ao.count(MailItemEntity.class)).thenReturn(1);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[0]);

        String json = service.getAllMailItemsAsJson(null, 100, 10);

        JSONObject result = new JSONObject(json);
        assertEquals(1, result.getInt("total"));
        assertEquals(0, result.getJSONArray("items").length());
    }

    @Test
    @DisplayName("getAllMailItemsAsJson: limit=0 — возвращает все записи")
    void getAllMailItemsAsJson_zeroLimit_returnsAll() throws JSONException {
        stubEntity(entity1, "uuid-1", "a@t.com", "x@t.com", "Тема 1", null);
        stubEntity(entity2, "uuid-2", "b@t.com", "y@t.com", "Тема 2", null);
        when(ao.count(MailItemEntity.class)).thenReturn(2);
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1, entity2});

        String json = service.getAllMailItemsAsJson(null, 0, 0);

        JSONObject result = new JSONObject(json);
        assertEquals(2, result.getJSONArray("items").length());
    }

    // ===== deleteMailItemById =====

    @Test
    @DisplayName("deleteMailItemById: существующий UUID — удаляет запись и возвращает true")
    void deleteMailItemById_found_deletesAndReturnsTrue() {
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[]{entity1});

        assertTrue(service.deleteMailItemById("uuid-1"));
        verify(ao).delete(entity1);
    }

    @Test
    @DisplayName("deleteMailItemById: несуществующий UUID — возвращает false без удаления")
    void deleteMailItemById_notFound_returnsFalse() {
        when(ao.find(eq(MailItemEntity.class), any(Query.class)))
                .thenReturn(new MailItemEntity[0]);

        assertFalse(service.deleteMailItemById("no-such-uuid"));
        verify(ao, never()).delete(any(MailItemEntity.class));
    }

    // ===== deleteAllMailItemsSafe =====

    @Test
    @DisplayName("deleteAllMailItemsSafe: при наличии записей — удаляет и возвращает true")
    void deleteAllMailItemsSafe_withItems_deletesAndReturnsTrue() {
        when(ao.find(MailItemEntity.class)).thenReturn(new MailItemEntity[]{entity1, entity2});

        assertTrue(service.deleteAllMailItemsSafe());
        verify(ao).delete(entity1, entity2);
    }

    @Test
    @DisplayName("deleteAllMailItemsSafe: пустая база — возвращает false без удаления")
    void deleteAllMailItemsSafe_emptyDb_returnsFalse() {
        when(ao.find(MailItemEntity.class)).thenReturn(new MailItemEntity[0]);

        assertFalse(service.deleteAllMailItemsSafe());
        verify(ao, never()).delete(any(MailItemEntity[].class));
    }

    @Test
    @DisplayName("deleteAllMailItemsSafe: исключение от AO — пробрасывается как RuntimeException")
    void deleteAllMailItemsSafe_aoThrows_wrapsInRuntimeException() {
        when(ao.find(MailItemEntity.class)).thenThrow(new RuntimeException("DB failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.deleteAllMailItemsSafe());
        assertTrue(ex.getMessage().contains("Failed to delete all mail items"));
    }

    // ===== loadTestData =====

    @Test
    @DisplayName("loadTestData: создаёт ровно 5 сущностей и сохраняет каждую")
    void loadTestData_creates5EntitiesAndSavesEach() {
        when(ao.count(MailItemEntity.class)).thenReturn(0);
        when(ao.create(MailItemEntity.class)).thenReturn(entity1);

        assertTrue(service.loadTestData());

        verify(ao, times(5)).create(MailItemEntity.class);
        verify(entity1, times(5)).save();
    }

    @Test
    @DisplayName("loadTestData: нумерация начинается от текущего количества + 1")
    void loadTestData_numberingStartsFromCurrentCount() {
        when(ao.count(MailItemEntity.class)).thenReturn(3);
        when(ao.create(MailItemEntity.class)).thenReturn(entity1);

        service.loadTestData();

        verify(entity1).setSubject("Тестовое письмо #4");
    }

    // ===== Вспомогательный метод =====

    private void stubEntity(MailItemEntity entity, String uuid, String from, String to,
                            String subject, String body) {
        lenient().when(entity.getUuid()).thenReturn(uuid);
        lenient().when(entity.getFrom()).thenReturn(from);
        lenient().when(entity.getTo()).thenReturn(to);
        lenient().when(entity.getCc()).thenReturn(null);
        lenient().when(entity.getBcc()).thenReturn(null);
        lenient().when(entity.getSubject()).thenReturn(subject);
        lenient().when(entity.getBody()).thenReturn(body);
        lenient().when(entity.getAttachmentsName()).thenReturn(null);
    }
}
