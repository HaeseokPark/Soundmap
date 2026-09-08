package com.soundmap.features;

import org.junit.jupiter.api.Test;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import nl.martijndwars.webpush.*;
import static org.junit.jupiter.api.Assertions.*;

class PushEncryptionTest {
    @Test void preparesEncryptedVapidRequestWithoutSendingIt() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        var generator=KeyPairGenerator.getInstance("ECDH","BC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        var server=generator.generateKeyPair();var browser=generator.generateKeyPair();
        var encode=Base64.getUrlEncoder().withoutPadding();
        String browserKey=encode.encodeToString(((org.bouncycastle.jce.interfaces.ECPublicKey)browser.getPublic()).getQ().getEncoded(false));
        String auth=encode.encodeToString(new byte[16]);
        var push=new nl.martijndwars.webpush.PushService(server,"mailto:test@example.com");
        var notification=new Notification("https://fcm.googleapis.com/fcm/send/test",browserKey,auth,"{\"title\":\"SoundMap\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var request=push.preparePost(notification,Encoding.AES128GCM);
        assertNotNull(request.getFirstHeader("Authorization"));
        assertEquals("aes128gcm",request.getFirstHeader("Content-Encoding").getValue());
        assertTrue(request.getEntity().getContentLength()>20);
    }
}
