package com.soundmap.features;

import com.soundmap.music.*;
import com.soundmap.music.dto.NowPlayingRequest;
import com.soundmap.music.entity.TrackSource;
import com.soundmap.music.repository.*;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import com.soundmap.chat.ChatService;
import com.soundmap.chat.ChatBroadcaster;
import com.soundmap.notification.*;
import com.soundmap.location.entity.UserLocation;
import com.soundmap.location.repository.UserLocationRepository;
import com.soundmap.settings.entity.UserSettings;
import com.soundmap.settings.ProfileScope;
import com.soundmap.settings.repository.UserSettingsRepository;
import com.soundmap.safety.SafetyService;
import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:features;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "soundmap.seed.enabled=false",
    "logging.level.org.hibernate.SQL=warn", "logging.level.org.hibernate.orm.jdbc.bind=warn",
    "PUSH_VAPID_PUBLIC_KEY=", "PUSH_VAPID_PRIVATE_KEY="
})
@Transactional
class MusicFeaturesIntegrationTest {
    @Autowired MusicService music;
    @Autowired MusicCollectionService collections;
    @Autowired UserRepository users;
    @Autowired UserLocationRepository locations;
    @Autowired UserSettingsRepository settings;
    @Autowired ChatService chats;
    @Autowired NotificationService notifications;
    @Autowired SafetyService safety;
    @Autowired MusicCheckInRepository history;
    @Autowired PushService push;
    @Autowired EntityManager em;
    Long alice,bob,eve;

    @BeforeEach void prepare() {
        alice=user("alice");bob=user("bob");eve=user("eve");
    }
    Long user(String name) {return users.save(User.builder().email(name+"@test.local").nickname(name).passwordHash("test").build()).getId();}
    NowPlayingRequest track(String id) {return new NowPlayingRequest(TrackSource.ITUNES,id,"Song "+id,"Artist "+id,"Album",null,null);}
    String month() {return YearMonth.now(ZoneId.of("Asia/Seoul")).toString();}
    void locate(Long user,String lat,String lon) {locations.save(new UserLocation(users.getReferenceById(user),new BigDecimal(lat),new BigDecimal(lon)));}
    void privateMusic(Long user) {var value=new UserSettings(users.getReferenceById(user));value.update(true,false,ProfileScope.PUBLIC);settings.save(value);}

    @Test void savesAreIdempotentAndPrivate() {
        Long id=music.checkIn(alice,track("1")).track().id();
        collections.save(alice,id);collections.save(alice,id);
        em.flush();em.clear();
        assertEquals(1,collections.saved(alice,0).size());
        assertTrue(collections.saved(bob,0).isEmpty());
        collections.unsave(bob,id);
        assertEquals(1,collections.saved(alice,0).size());
        collections.unsave(alice,id);
        assertTrue(collections.savedIds(alice).isEmpty());
    }
    @Test void checkInsRemainAfterCurrentTrackChangesAndAreOwnerOnly() {
        music.checkIn(alice,track("1"));music.checkIn(alice,track("2"));music.clearNowPlaying(alice);
        var result=collections.history(alice,month(),0);
        assertEquals(2,result.entries().size());assertEquals(2,result.artists().size());
        Long id=result.entries().getFirst().id();collections.deleteHistory(bob,id);
        assertEquals(2,collections.history(alice,month(),0).entries().size());
        collections.deleteHistory(alice,id);
        assertEquals(1,collections.history(alice,month(),0).entries().size());
        assertTrue(collections.history(bob,month(),0).entries().isEmpty());
    }
    @Test void historyMonthAndPaginationAreValidated() {
        assertThrows(ApiException.class,()->collections.history(alice,"invalid",0));
        assertThrows(ApiException.class,()->collections.saved(alice,-1));
        for(int i=0;i<51;i++)music.checkIn(alice,track("1"));
        assertEquals(50,collections.history(alice,month(),0).entries().size());
        assertTrue(collections.history(alice,month(),0).hasMore());
        assertEquals(1,collections.history(alice,month(),1).entries().size());
        assertTrue(collections.history(alice,YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1).toString(),0).entries().isEmpty());
    }
    @Test void chartCountsDistinctPeopleIncludingMeAndHonorsPrivacyAndBlocks() {
        locate(alice,"36.103","129.389");locate(bob,"36.104","129.389");locate(eve,"36.105","129.389");
        music.checkIn(alice,track("1"));music.checkIn(alice,track("1"));music.checkIn(bob,track("1"));music.checkIn(eve,track("1"));
        assertEquals(3,collections.chart(alice,2000).getFirst().people());
        privateMusic(eve);
        assertEquals(2,collections.chart(alice,2000).getFirst().people());
        safety.block(alice,bob);
        assertEquals(1,collections.chart(alice,2000).getFirst().people());
        locations.deleteById(alice);
        assertTrue(collections.chart(alice,2000).isEmpty());
    }
    @Test void chartRejectsStaleAndOutsideRadiusLocations() {
        locate(alice,"36.103","129.389");locate(bob,"37.500","127.000");locate(eve,"36.104","129.389");
        music.checkIn(bob,track("1"));music.checkIn(eve,track("2"));
        locations.touchAll(java.util.List.of(eve),LocalDateTime.now().minusDays(2));
        assertTrue(collections.chart(alice,5000).isEmpty());
    }
    @Test void previouslyPrivateCheckInsNeverBecomePublicChartEntries() {
        locate(alice,"36.103","129.389");locate(bob,"36.104","129.389");
        privateMusic(bob);music.checkIn(bob,track("private"));
        settings.findById(bob).orElseThrow().update(true,true,ProfileScope.PUBLIC);
        assertTrue(collections.chart(alice,2000).isEmpty());
        assertEquals(1,collections.history(bob,month(),0).entries().size());
    }
    @Test void musicCardsRequireAcceptanceAndMembershipAndSurviveReload() {
        Long room=chats.request(alice,bob).roomId();
        assertEquals(ErrorCode.CHAT_NOT_ACCEPTED,assertThrows(ApiException.class,()->chats.sendTrack(alice,room,track("1"))).getErrorCode());
        chats.accept(bob,room);
        assertThrows(ApiException.class,()->chats.sendTrack(eve,room,track("1")));
        var message=chats.sendTrack(alice,room,track("1"));
        assertNotNull(message.track());assertEquals("Song 1",message.track().title());
        em.flush();em.clear();
        var reloaded=chats.findMessages(bob,room,null,null).getFirst();
        assertFalse(reloaded.mine());assertEquals(message.track().id(),reloaded.track().id());
        assertTrue(collections.history(alice,month(),0).entries().isEmpty());
        safety.block(bob,alice);
        assertThrows(ApiException.class,()->chats.sendTrack(alice,room,track("2")));
    }
    @Test void notificationsAreDurableDeduplicatedForRequestsAndOwnerScoped() {
        Long room=chats.request(alice,bob).roomId();chats.request(alice,bob);
        assertEquals(1,notifications.inbox(bob,0).unreadCount());
        chats.accept(bob,room);chats.accept(bob,room);
        assertEquals(1,notifications.inbox(alice,0).unreadCount());
        chats.sendMessage(alice,room,"hello");
        assertEquals(2,notifications.inbox(bob,0).unreadCount());
        Long id=notifications.inbox(bob,0).items().getFirst().getId();
        assertThrows(ApiException.class,()->notifications.read(eve,id));
        notifications.read(bob,id);assertEquals(1,notifications.inbox(bob,0).unreadCount());
        notifications.readAll(bob);assertEquals(0,notifications.inbox(bob,0).unreadCount());
        safety.block(bob,alice);assertTrue(notifications.inbox(bob,0).items().isEmpty());
    }
    @Test void disabledPushDoesNotPreventInbox() {
        assertEquals(false,push.config().get("enabled"));
        assertThrows(ApiException.class,()->push.subscribe(alice,"https://fcm.googleapis.com/test","invalid","invalid"));
        chats.request(alice,bob);assertEquals(1,notifications.inbox(bob,0).items().size());
    }
    @Test void websocketMusicPayloadIncludesTrack() {
        var template=org.mockito.Mockito.mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);
        var broadcaster=new ChatBroadcaster(template);
        Long room=chats.request(alice,bob).roomId();chats.accept(bob,room);
        var message=chats.sendTrack(alice,room,track("1"));broadcaster.broadcastMessage(room,message);
        var capture=org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(template).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/chats/"+room),capture.capture());
        assertEquals(message.track(),((ChatBroadcaster.SocketMessage)capture.getValue()).track());
    }
}
