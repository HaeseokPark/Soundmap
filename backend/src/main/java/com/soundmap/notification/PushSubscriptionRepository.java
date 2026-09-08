package com.soundmap.notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription,String> {
    List<PushSubscription> findByUserId(Long userId);
    void deleteByIdAndUserId(String id,Long userId);
    void deleteByUserId(Long userId);
}
