package com.soundmap.dev;

import com.soundmap.taste.entity.Artist;
import com.soundmap.taste.entity.Genre;
import com.soundmap.taste.repository.ArtistRepository;
import com.soundmap.taste.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

/**
 * 가수 · 장르 기준 데이터.
 *
 * <p>더미 사용자와 달리 이건 운영에서도 필요한 데이터라 항상 실행한다.
 * 이미 있는 항목은 건너뛰므로 여러 번 떠도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class ReferenceDataSeeder implements ApplicationRunner {

    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int genres = seedGenres();
        int artists = seedArtists();

        if (genres > 0 || artists > 0) {
            log.info("기준 데이터를 추가했습니다. 장르 {}개, 가수 {}명", genres, artists);
        }
    }

    private int seedGenres() {
        GenreSeed[] seeds = read("seed/genres.json", GenreSeed[].class);
        int added = 0;
        for (GenreSeed seed : seeds) {
            if (genreRepository.findByName(seed.name()).isEmpty()) {
                genreRepository.save(new Genre(seed.name()));
                added++;
            }
        }
        return added;
    }

    private int seedArtists() {
        ArtistSeed[] seeds = read("seed/artists.json", ArtistSeed[].class);
        int added = 0;
        for (ArtistSeed seed : seeds) {
            if (artistRepository.findByName(seed.name()).isEmpty()) {
                artistRepository.save(Artist.builder()
                        .name(seed.name())
                        .imageUrl(seed.imageUrl())
                        .build());
                added++;
            }
        }
        return added;
    }

    private <T> T read(String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (Exception e) {
            throw new IllegalStateException("기준 데이터 파일을 읽지 못했습니다: " + path, e);
        }
    }

    private record GenreSeed(String name) {
    }

    private record ArtistSeed(String name, String imageUrl, String primaryGenre) {
    }
}
