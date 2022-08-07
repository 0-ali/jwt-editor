package com.blackberry.jwteditor;

import burp.IBurpExtenderCallbacks;
import com.blackberry.jwteditor.model.config.HighlightColor;
import com.blackberry.jwteditor.model.config.ProxyConfig;
import com.blackberry.jwteditor.model.persistence.ProxyConfigPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.blackberry.jwteditor.model.config.ProxyConfig.DEFAULT_HIGHLIGHT_COLOR;
import static com.blackberry.jwteditor.model.persistence.ProxyConfigPersistence.PROXY_LISTENER_SETTINGS_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyConfigPersistenceTest {
    private final IBurpExtenderCallbacks callbacks = mock(IBurpExtenderCallbacks.class);

    @Test
    void givenNoSavedConfig_whenLoadOrCreateCalled_thenDefaultInstanceReturned() {
        ProxyConfigPersistence configPersistence = new ProxyConfigPersistence(callbacks);

        ProxyConfig proxyConfig = configPersistence.loadOrCreateNew();

        assertNotNull(proxyConfig);
        assertTrue(proxyConfig.highlightJWT());
        assertEquals(proxyConfig.highlightColor(), DEFAULT_HIGHLIGHT_COLOR);
        configPersistence.save(proxyConfig);
    }

    private static Stream<String> invalidProxyConfigJson() {
        return Stream.of(
                "",
                "{",
                "]",
                "true",
                "[]",
                "null",
                "{\"proxy_history_highlight_color\":\"red\",\"proxy_listener_enabled\":[]}",
                "{\"proxy_history_highlight_color\":\"red\"}",
                "{\"proxy_listener_enabled\":true}",
                "{\"proxy_history_highlight_color\":\"gunmetal\",\"proxy_listener_enabled\":true}",
                "{\"proxy_history_highlight_color\":\"red\",\"proxy_listener_enabled\":\"turnip\"}"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidProxyConfigJson")
    void givenInvalidSavedConfig_whenLoadOrCreateCalled_thenDefaultInstanceReturned(String invalidJson) {
        ProxyConfigPersistence configPersistence = new ProxyConfigPersistence(callbacks);
        when(callbacks.loadExtensionSetting(PROXY_LISTENER_SETTINGS_NAME)).thenReturn(invalidJson);

        ProxyConfig proxyConfig = configPersistence.loadOrCreateNew();

        assertNotNull(proxyConfig);
        assertTrue(proxyConfig.highlightJWT());
        assertEquals(proxyConfig.highlightColor(), DEFAULT_HIGHLIGHT_COLOR);
    }

    private static Stream<Arguments> validProxyConfigJson() {
        return Stream.of(
                arguments(
                        "{\"proxy_history_highlight_color\":\"red\",\"proxy_listener_enabled\":true}",
                        true,
                        HighlightColor.RED
                ),
                arguments(
                        "{\"proxy_history_highlight_color\":\"cyan\",\"proxy_listener_enabled\":false}",
                        false,
                        HighlightColor.CYAN
                )
        );
    }

    @ParameterizedTest
    @MethodSource("validProxyConfigJson")
    void givenValidSavedConfig_whenLoadOrCreateCalled_thenAppropriateInstanceReturned(String json, boolean listenerEnabled, HighlightColor highlightColor) {
        ProxyConfigPersistence configPersistence = new ProxyConfigPersistence(callbacks);
        when(callbacks.loadExtensionSetting(PROXY_LISTENER_SETTINGS_NAME)).thenReturn(json);

        ProxyConfig proxyConfig = configPersistence.loadOrCreateNew();

        assertNotNull(proxyConfig);
        assertEquals(proxyConfig.highlightJWT(), listenerEnabled);
        assertEquals(proxyConfig.highlightColor(), highlightColor);
    }
}