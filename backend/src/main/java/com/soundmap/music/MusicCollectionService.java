package com.soundmap.music;
import com.soundmap.music.entity.*;
import com.soundmap.music.dto.TrackResponse;
import com.soundmap.music.repository.*;
import com.soundmap.location.*;
import com.soundmap.location.repository.UserLocationRepository;
import com.soundmap.safety.SafetyService;
import com.soundmap.settings.SettingsService;
import com.soundmap.settings.ProfileScope;
import com.soundmap.global.error.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.soundmap.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class MusicCollectionService {
    private final SavedTrackRepository saved;
    private final MusicCheckInRepository history;
    private final TrackRepository tracks;
    private final UserLocationRepository locations;
    private final LocationProperties properties;
    private final SafetyService safety;
    private final com.soundmap.settings.repository.UserSettingsRepository settings;
    private final EntityManager em;
    public record Entry(Long id, TrackResponse track, LocalDateTime createdAt) {}
    public record ArtistCount(String artistName, long count) {}
    public record HistoryPage(List<Entry> entries, List<ArtistCount> artists, boolean hasMore) {}
    public record ChartEntry(TrackResponse track, long people) {}
    public List<Entry> saved(Long user, int page) {
        return saved.findByUserIdOrderByIdDesc(user, page(page)).stream().map(s->new Entry(s.getId(),TrackResponse.from(s.getTrack()),s.getCreatedAt())).toList();
    }
    public List<Long> savedIds(Long user) { return saved.trackIds(user); }
    @Transactional public void save(Long user, Long track) {
        em.find(User.class,user,LockModeType.PESSIMISTIC_WRITE);
        Track found=tracks.findById(track).orElseThrow(()->new ApiException(ErrorCode.INVALID_INPUT));
        if(!saved.existsByUserIdAndTrackId(user,track)) saved.save(new SavedTrack(user,found));
    }
    @Transactional public void unsave(Long user, Long track) { saved.deleteByUserIdAndTrackId(user,track); }
    @Transactional public void deleteHistory(Long user,Long id) { history.deleteByIdAndUserId(id,user); }
    public HistoryPage history(Long user, String month, int page) {
        YearMonth ym;
        try { ym=month==null?YearMonth.now(ZoneId.of("Asia/Seoul")):YearMonth.parse(month); }
        catch(Exception e) { throw new ApiException(ErrorCode.INVALID_INPUT); }
        var start=ym.atDay(1).atStartOfDay(); var end=ym.plusMonths(1).atDay(1).atStartOfDay();
        var entries=history.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(user,start,end,page(page));
        var artists=history.artists(user,start,end).stream().limit(10).map(a->new ArtistCount((String)a[0],((Number)a[1]).longValue())).toList();
        return new HistoryPage(entries.stream().map(c->new Entry(c.getId(),TrackResponse.from(c.getTrack()),c.getCreatedAt())).toList(),artists,entries.size()==50);
    }
    public List<ChartEntry> chart(Long user,int radius) {
        var me=locations.findById(user).orElse(null);
        if(me==null) return List.of();
        int meters=Math.max(500,Math.min(radius,5000));
        var box=GeoDistance.boundingBox(me.getGridLat().doubleValue(),me.getGridLon().doubleValue(),meters);
        Set<Long> hidden=new HashSet<>(safety.hiddenUserIdsFor(user)); hidden.add(0L);
        var candidates=locations.findInBoundingBox(box.minLat(),box.maxLat(),box.minLon(),box.maxLon(),user,LocalDateTime.now().minus(properties.pinTtl()),hidden);
        var nearby=new ArrayList<Long>(candidates.stream().filter(l->GeoDistance.haversineMeters(me.getGridLat(),me.getGridLon(),l.getGridLat(),l.getGridLon())<=meters)
            .map(l->l.getUserId()).toList());
        if(me.getUpdatedAt().isAfter(LocalDateTime.now().minus(properties.pinTtl()))) nearby.add(user);
        Set<Long> excluded=new HashSet<>();
        settings.findAllById(nearby).stream().filter(s->!s.isShareLocation()||!s.isShareNowPlaying()||s.getProfileScope()!=ProfileScope.PUBLIC).forEach(s->excluded.add(s.getUserId()));
        var ids=nearby.stream().filter(id->!excluded.contains(id)).toList();
        if(ids.isEmpty()) return List.of();
        var start=LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();
        var rows=history.chart(ids,start,start.plusDays(1),PageRequest.of(0,20));
        Map<Long,Track> byId=new HashMap<>(); tracks.findAllById(rows.stream().map(r->(Long)r[0]).toList()).forEach(t->byId.put(t.getId(),t));
        return rows.stream().filter(r->byId.containsKey((Long)r[0])).map(r->new ChartEntry(TrackResponse.from(byId.get((Long)r[0])),((Number)r[1]).longValue())).toList();
    }
    private PageRequest page(int page) { if(page<0||page>10000) throw new ApiException(ErrorCode.INVALID_INPUT); return PageRequest.of(page,50); }
}
