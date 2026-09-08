package com.soundmap.notification;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="push_subscriptions",indexes=@Index(columnList="user_id"))
public class PushSubscription {
    @Id @Column(length=64) private String id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=2048) private String endpoint;
    @Column(nullable=false,length=200) private String publicKey;
    @Column(nullable=false,length=100) private String auth;
    public PushSubscription(String id,Long user,String endpoint,String key,String auth) {
        this.id=id;userId=user;this.endpoint=endpoint;publicKey=key;this.auth=auth;
    }
}
