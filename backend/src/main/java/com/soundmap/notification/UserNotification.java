package com.soundmap.notification;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="user_notifications",indexes=@Index(columnList="user_id,id"))
public class UserNotification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private Long roomId;
    @Column(nullable=false,length=20) private String type;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime readAt;
    public UserNotification(Long user,Long actor,Long room,String type,String title) {
        userId=user; actorId=actor; roomId=room; this.type=type; this.title=title; createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
    }
    public void markRead() { if(readAt==null) readAt=LocalDateTime.now(); }
}
