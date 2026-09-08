package com.soundmap.notification;
import com.soundmap.safety.SafetyService;
import com.soundmap.global.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class NotificationService {
    private final NotificationRepository repository;
    private final SafetyService safety;
    private final ApplicationEventPublisher events;
    public record Created(Long id,Long user,Long actor,Long room,String title) {}
    public record Inbox(List<UserNotification> items,long unreadCount,boolean hasMore) {}
    @Transactional public void create(Long user,Long actor,Long room,String type,String title) {
        var saved=repository.save(new UserNotification(user,actor,room,type,title));
        events.publishEvent(new Created(saved.getId(),user,actor,room,title));
    }
    public Inbox inbox(Long user,int page) {
        if(page<0||page>10000) throw new ApiException(ErrorCode.INVALID_INPUT);
        var hidden=new HashSet<>(safety.hiddenUserIdsFor(user)); hidden.add(0L);
        var items=repository.inbox(user,hidden,PageRequest.of(page,50));
        return new Inbox(items,repository.unread(user,hidden),items.size()==50);
    }
    @Transactional public void read(Long user,Long id) {
        var n=repository.findByIdAndUserId(id,user).orElseThrow(()->new ApiException(ErrorCode.INVALID_INPUT));
        if(safety.isBlockedBetween(user,n.getActorId())) throw new ApiException(ErrorCode.INVALID_INPUT);
        n.markRead();
    }
    @Transactional public void readAll(Long user) {repository.readAll(user,LocalDateTime.now());}
}
