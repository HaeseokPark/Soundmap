package com.soundmap.music;

import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.music.client.ITunesClient;
import com.soundmap.music.dto.NowPlayingRequest;
import com.soundmap.music.dto.NowPlayingResponse;
import com.soundmap.music.dto.TrackSearchResult;
import com.soundmap.music.entity.NowPlaying;
import com.soundmap.music.entity.Track;
import com.soundmap.music.repository.NowPlayingRepository;
import com.soundmap.music.repository.TrackRepository;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicService {

    private final com.soundmap.music.repository.MusicCheckInRepository historyRepository;
    private final com.soundmap.settings.SettingsService settingsService;
    private final ITunesClient iTunesClient;
    private final TrackRepository trackRepository;
    private final NowPlayingRepository nowPlayingRepository;
    private final UserRepository userRepository;

    public List<TrackSearchResult> search(String term) {
        String trimmed = term == null ? "" : term.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return iTunesClient.searchSongs(trimmed);
    }

    @Transactional
    public NowPlayingResponse checkIn(Long userId, NowPlayingRequest request) {
        Track track = findOrCreateTrack(request);

        NowPlaying nowPlaying = nowPlayingRepository.findById(userId)
                .map(existing -> {
                    existing.changeTrack(track);
                    return existing;
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
                    return nowPlayingRepository.save(new NowPlaying(user, track));
                });

        boolean publiclyShared=!settingsService.hidesNowPlaying(userId)
                && settingsService.profileScopeOf(userId)==com.soundmap.settings.ProfileScope.PUBLIC;
        historyRepository.save(new com.soundmap.music.entity.MusicCheckIn(userId, track, publiclyShared));
        return NowPlayingResponse.from(nowPlaying);
    }

    public Optional<NowPlayingResponse> findMyNowPlaying(Long userId) {
        return nowPlayingRepository.findWithTrackByUserId(userId).map(NowPlayingResponse::from);
    }

    /** 현재 음악 공개 끄기. */
    @Transactional
    public void clearNowPlaying(Long userId) {
        nowPlayingRepository.deleteById(userId);
    }

    /**
     * 같은 곡은 한 행만 유지한다. 이미 있으면 재사용하고 없을 때만 저장한다.
     */
    @Transactional
    public Track findOrCreateTrack(NowPlayingRequest request) {
        return trackRepository.findBySourceAndExternalId(request.source(), request.externalId())
                .map(existing -> {
                    // 미리듣기가 생기기 전에 저장된 곡은 주소가 비어 있다. 이때 메운다.
                    existing.fillPreviewUrlIfAbsent(request.previewUrl());
                    return existing;
                })
                .orElseGet(() -> trackRepository.save(Track.builder()
                        .source(request.source())
                        .externalId(request.externalId())
                        .title(request.title())
                        .artistName(request.artistName())
                        .albumName(request.albumName())
                        .albumArtUrl(request.albumArtUrl())
                        .previewUrl(request.previewUrl())
                        .build()));
    }
}
