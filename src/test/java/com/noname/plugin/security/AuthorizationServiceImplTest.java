package com.noname.plugin.security;

import com.atlassian.sal.api.user.UserManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationServiceImpl — авторизация через SAL UserManager")
class AuthorizationServiceImplTest {

    @Mock
    private UserManager userManager;

    // Тест 1: null UserManager — ранний возврат на строке 28, ComponentAccessor не вызывается
    @Test
    @DisplayName("isSystemAdmin() возвращает false, если UserManager равен null")
    void isSystemAdmin_whenUserManagerNull_returnsFalse() {
        AuthorizationServiceImpl service = new AuthorizationServiceImpl(null);
        assertFalse(service.isSystemAdmin());
    }

    // Тест 2: getRemoteUserKey() возвращает null — ранний возврат по проверке userKey != null
    @Test
    @DisplayName("isSystemAdmin() возвращает false, если getRemoteUserKey() равен null")
    void isSystemAdmin_whenNoRemoteUser_returnsFalse() {
        when(userManager.getRemoteUserKey()).thenReturn(null);
        AuthorizationServiceImpl service = new AuthorizationServiceImpl(userManager);
        assertFalse(service.isSystemAdmin());
    }

    // Тест 3: валидный UserKey и isSystemAdmin(key) == true — ComponentAccessor больше не вызывается
    @Test
    @DisplayName("isSystemAdmin() возвращает true, если UserManager подтверждает системного администратора")
    void isSystemAdmin_whenUserIsAdmin_returnsTrue() {
        com.atlassian.sal.api.user.UserKey key =
                new com.atlassian.sal.api.user.UserKey("admin");
        when(userManager.getRemoteUserKey()).thenReturn(key);
        when(userManager.isSystemAdmin(key)).thenReturn(true);
        AuthorizationServiceImpl service = new AuthorizationServiceImpl(userManager);
        assertTrue(service.isSystemAdmin());
    }
}
