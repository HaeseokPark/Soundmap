package com.soundmap.notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.time.LocalDateTime;
public interface NotificationRepository extends JpaRepository<UserNotification,Long> {
    @Query("select n from UserNotification n where n.userId=:user and n.actorId not in :hidden order by n.id desc")
    List<UserNotification> inbox(Long user,Collection<Long> hidden,Pageable page);
    @Query("select count(n) from UserNotification n where n.userId=:user and n.actorId not in :hidden and n.readAt is null")
    long unread(Long user,Collection<Long> hidden);
    Optional<UserNotification> findByIdAndUserId(Long id,Long userId);
    @Modifying @Query("update UserNotification n set n.readAt=:now where n.userId=:user and n.readAt is null")
    void readAll(Long user,LocalDateTime now);
}
