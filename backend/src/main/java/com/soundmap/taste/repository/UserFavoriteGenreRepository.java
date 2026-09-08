package com.soundmap.taste.repository;

import com.soundmap.taste.entity.Genre;
import com.soundmap.taste.entity.UserFavoriteGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFavoriteGenreRepository
        extends JpaRepository<UserFavoriteGenre, UserFavoriteGenre.Key> {

    @Query("""
            select g from Genre g
            where g.id in (
                select ufg.id.genreId from UserFavoriteGenre ufg
                where ufg.id.userId = :userId
            )
            order by g.id
            """)
    List<Genre> findGenresByUserId(@Param("userId") Long userId);

    void deleteByIdUserId(Long userId);
}
