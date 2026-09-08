package com.soundmap.music.repository;

import com.soundmap.music.entity.Track;
import com.soundmap.music.entity.TrackSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    Optional<Track> findBySourceAndExternalId(TrackSource source, String externalId);
}
