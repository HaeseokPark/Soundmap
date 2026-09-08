package com.soundmap.notification;
import com.soundmap.global.jwt.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;
@RestController @RequestMapping("/api/me/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    private final PushService push;
    public record Keys(@NotBlank @Size(max=200) String p256dh,@NotBlank @Size(max=100) String auth) {}
    public record Subscription(@NotBlank @Size(max=2048) String endpoint,@NotNull @Valid Keys keys) {}
    public record Endpoint(@NotBlank @Size(max=2048) String endpoint) {}
    @GetMapping public NotificationService.Inbox inbox(@AuthenticationPrincipal AuthPrincipal me,@RequestParam(defaultValue="0") int page) {return service.inbox(me.userId(),page);}
    @PostMapping("/{id}/read") public void read(@AuthenticationPrincipal AuthPrincipal me,@PathVariable Long id) {service.read(me.userId(),id);}
    @PostMapping("/read-all") public void all(@AuthenticationPrincipal AuthPrincipal me) {service.readAll(me.userId());}
    @GetMapping("/push-config") public Map<String,Object> config() {return push.config();}
    @PutMapping("/push") public void subscribe(@AuthenticationPrincipal AuthPrincipal me,@Valid @RequestBody Subscription sub) {push.subscribe(me.userId(),sub.endpoint(),sub.keys().p256dh(),sub.keys().auth());}
    @DeleteMapping("/push") public void unsubscribe(@AuthenticationPrincipal AuthPrincipal me,@Valid @RequestBody Endpoint sub) {push.unsubscribe(me.userId(),sub.endpoint());}
    @DeleteMapping("/push/all") public void unsubscribeAll(@AuthenticationPrincipal AuthPrincipal me) {push.unsubscribeAll(me.userId());}
}
