package com.soundmap.taste.repository;

import com.soundmap.taste.entity.Artist;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByName(String name);

    /**
     * 온보딩 화면의 가수 검색.
     *
     * <p>LIKE '%...%' 는 인덱스를 타지 못한다. 아티스트 목록이 수백 건 수준이라
     * 지금은 문제가 없지만, 수만 건으로 늘리면 FULLTEXT(ngram 파서)로 바꿔야 한다.
     */
    List<Artist> findByNameContainingIgnoreCaseOrderByName(String keyword, Limit limit);

    List<Artist> findAllByOrderByName(Limit limit);

    long countByIdIn(Collection<Long> ids);
}
