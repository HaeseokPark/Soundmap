package com.soundmap.taste.repository;

import com.soundmap.taste.entity.Artist;
import com.soundmap.taste.entity.UserFavoriteArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFavoriteArtistRepository
        extends JpaRepository<UserFavoriteArtist, UserFavoriteArtist.Key> {

    /** 이 사용자가 좋아하는 가수들. 화면에 이름과 이미지를 써야 하므로 Artist 로 바로 받는다. */
    @Query("""
            select a from Artist a
            where a.id in (
                select ufa.id.artistId from UserFavoriteArtist ufa
                where ufa.id.userId = :userId
            )
            order by a.name
            """)
    List<Artist> findArtistsByUserId(@Param("userId") Long userId);

    @Query("select ufa.id.artistId from UserFavoriteArtist ufa where ufa.id.userId = :userId")
    List<Long> findArtistIdsByUserId(@Param("userId") Long userId);

    void deleteByIdUserId(Long userId);

    boolean existsByIdUserId(Long userId);
}
