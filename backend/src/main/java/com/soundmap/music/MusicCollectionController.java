package com.soundmap.music;
import com.soundmap.global.jwt.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class MusicCollectionController {
    private final MusicCollectionService service;
    @GetMapping("/me/saved-tracks") public List<MusicCollectionService.Entry> saved(@AuthenticationPrincipal AuthPrincipal me,@RequestParam(defaultValue="0") int page) {return service.saved(me.userId(),page);}
    @GetMapping("/me/saved-tracks/ids") public List<Long> ids(@AuthenticationPrincipal AuthPrincipal me) {return service.savedIds(me.userId());}
    @PutMapping("/me/saved-tracks/{id}") public void save(@AuthenticationPrincipal AuthPrincipal me,@PathVariable Long id) {service.save(me.userId(),id);}
    @DeleteMapping("/me/saved-tracks/{id}") public void unsave(@AuthenticationPrincipal AuthPrincipal me,@PathVariable Long id) {service.unsave(me.userId(),id);}
    @GetMapping("/me/music-history") public MusicCollectionService.HistoryPage history(@AuthenticationPrincipal AuthPrincipal me,@RequestParam(required=false) String month,@RequestParam(defaultValue="0") int page) {return service.history(me.userId(),month,page);}
    @DeleteMapping("/me/music-history/{id}") public void delete(@AuthenticationPrincipal AuthPrincipal me,@PathVariable Long id) {service.deleteHistory(me.userId(),id);}
    @GetMapping("/music/local-chart") public List<MusicCollectionService.ChartEntry> chart(@AuthenticationPrincipal AuthPrincipal me,@RequestParam(defaultValue="2000") int radiusMeters) {return service.chart(me.userId(),radiusMeters);}
}
