package com.soundmap.notification;
import com.soundmap.global.error.*;
import com.soundmap.safety.SafetyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Encoding;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import tools.jackson.databind.ObjectMapper;
@Service @RequiredArgsConstructor @Slf4j
public class PushService {
    private final PushSubscriptionRepository subscriptions;
    private final SafetyService safety;
    private final SimpMessagingTemplate socket;
    private final ObjectMapper json;
    @Value("${PUSH_VAPID_PUBLIC_KEY:}") private String publicKey;
    @Value("${PUSH_VAPID_PRIVATE_KEY:}") private String privateKey;
    @Value("${PUSH_VAPID_SUBJECT:}") private String subject;
    public boolean configured() {return !publicKey.isBlank()&&!privateKey.isBlank()&&!subject.isBlank();}
    public Map<String,Object> config() { return Map.of("enabled",configured(),"publicKey",configured()?publicKey:""); }
    public static String endpointId(String endpoint) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    // Only known browser push gateways are accepted; arbitrary URLs must never reach the HTTP client.
    public static boolean validEndpoint(String endpoint) {
        try {
            URI uri=URI.create(endpoint); String host=uri.getHost();
            return "https".equals(uri.getScheme())&&uri.getUserInfo()==null&&uri.getFragment()==null&&(uri.getPort()==-1||uri.getPort()==443)
                &&host!=null&&(host.equals("fcm.googleapis.com")||host.equals("updates.push.services.mozilla.com")
                ||host.endsWith(".push.services.mozilla.com")||host.equals("web.push.apple.com")||host.endsWith(".notify.windows.com"));
        } catch(Exception e) {return false;}
    }
    @Transactional public void subscribe(Long user,String endpoint,String key,String auth) {
        if(!configured()||!validEndpoint(endpoint)) throw new ApiException(ErrorCode.INVALID_INPUT);
        try {
            byte[] decoded=Base64.getUrlDecoder().decode(key);
            if(decoded.length!=65||decoded[0]!=4||Base64.getUrlDecoder().decode(auth).length!=16) throw new IllegalArgumentException();
        } catch(Exception e) {throw new ApiException(ErrorCode.INVALID_INPUT);}
        subscriptions.save(new PushSubscription(endpointId(endpoint),user,endpoint,key,auth));
    }
    @Transactional public void unsubscribe(Long user,String endpoint) {subscriptions.deleteByIdAndUserId(endpointId(endpoint),user);}
    @Transactional public void unsubscribeAll(Long user) {subscriptions.deleteByUserId(user);}
    @Async("notificationExecutor") @TransactionalEventListener
    public void deliver(NotificationService.Created event) {
        if(safety.isBlockedBetween(event.user(),event.actor())) return;
        socket.convertAndSendToUser(event.user().toString(),"/queue/notifications",Map.of("id",event.id()));
        if(!configured()) return;
        try {
            Security.addProvider(new BouncyCastleProvider());
            var sender=new nl.martijndwars.webpush.PushService(publicKey,privateKey,subject);
            byte[] payload=json.writeValueAsBytes(Map.of("title","잔향","body",event.title(),"url","/chats/"+event.room(),"tag","room-"+event.room()));
            var config=RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(5000).setConnectionRequestTimeout(5000).build();
            try(var client=HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(config).build()) {
                for(var sub:subscriptions.findByUserId(event.user())) {
                    if(!validEndpoint(sub.getEndpoint())) continue;
                    try {
                        var request=sender.preparePost(new Notification(sub.getEndpoint(),sub.getPublicKey(),sub.getAuth(),payload),Encoding.AES128GCM);
                        try(var response=client.execute(request)) {
                            int status=response.getStatusLine().getStatusCode();
                            if(status==404||status==410) subscriptions.deleteById(sub.getId());
                            else if(status>=400) log.warn("Push delivery failed: status={}",status);
                        }
                    } catch(Exception e) {log.warn("Push delivery failed for notification {}",event.id());}
                }
            }
        } catch(Exception e) {log.warn("Push configuration or delivery failed for notification {}",event.id());}
    }
}
