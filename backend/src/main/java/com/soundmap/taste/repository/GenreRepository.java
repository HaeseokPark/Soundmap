package com.soundmap.taste.repository;

import com.soundmap.taste.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByName(String name);

    long countByIdIn(Collection<Long> ids);
}
