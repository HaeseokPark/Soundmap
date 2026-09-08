package com.soundmap.taste.repository;

import com.soundmap.taste.entity.UserFavoriteArtist;
import com.soundmap.taste.entity.UserFavoriteGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 여러 사용자의 취향을 한 번에 가져오는 조회 전용 쿼리.
 *
 * <p>지도에 40명을 그릴 때 사람마다 취향을 조회하면 쿼리가 40번 나간다(N+1).
 * userId 목록을 IN 으로 묶어 두 번(가수 · 장르)만 나가게 한다.
 */
public interface TasteQueryRepository extends JpaRepository<UserFavoriteArtist, UserFavoriteArtist.Key> {

    @Query("""
            select ufa.id.userId, ufa.id.artistId
            from UserFavoriteArtist ufa
            where ufa.id.userId in :userIds
            """)
    List<Object[]> findArtistPairsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("""
            select ufg.id.userId, ufg.id.genreId
            from UserFavoriteGenre ufg
            where ufg.id.userId in :userIds
            """)
    List<Object[]> findGenrePairsByUserIds(@Param("userIds") Collection<Long> userIds);

    /** 이 가수를 좋아하는 사용자들. artist_id 인덱스를 탄다. */
    @Query("select ufa.id.userId from UserFavoriteArtist ufa where ufa.id.artistId = :artistId")
    List<Long> findUserIdsByArtistId(@Param("artistId") Long artistId);

    /** 이 장르를 좋아하는 사용자들. genre_id 인덱스를 탄다. */
    @Query("select ufg.id.userId from UserFavoriteGenre ufg where ufg.id.genreId = :genreId")
    List<Long> findUserIdsByGenreId(@Param("genreId") Long genreId);
}
