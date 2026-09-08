package com.soundmap.features;
import com.soundmap.notification.PushService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PushEndpointTest {
    @Test void acceptsBrowserGatewaysOnly() {
        assertTrue(PushService.validEndpoint("https://fcm.googleapis.com/fcm/send/token"));
        assertTrue(PushService.validEndpoint("https://updates.push.services.mozilla.com/wpush/v2/token"));
        assertTrue(PushService.validEndpoint("https://web.push.apple.com/token"));
        for(String endpoint:new String[]{"http://fcm.googleapis.com/test","https://127.0.0.1/test","https://localhost/test","https://fcm.googleapis.com.evil.test/token","https://user@fcm.googleapis.com/test","https://fcm.googleapis.com:8443/test","https://example.com/","not a url"})
            assertFalse(PushService.validEndpoint(endpoint),endpoint);
    }
}
