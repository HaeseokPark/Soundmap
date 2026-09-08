package com.soundmap.user.repository;

import com.soundmap.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** 시드가 만든 더미 사용자를 찾는 데 쓴다. 도메인으로 구분한다. */
    List<User> findByEmailEndingWith(String suffix);

    Optional<User> findByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
