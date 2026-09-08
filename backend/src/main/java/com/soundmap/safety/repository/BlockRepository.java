package com.soundmap.safety.repository;

import com.soundmap.safety.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Block.Key> {

    /**
     * 나와 차단 관계에 있는 모든 사용자 id (내가 차단한 사람 + 나를 차단한 사람).
     *
     * <p>차단은 양방향으로 동작하므로 두 방향을 합쳐서 본다. 지도·검색·채팅이
     * 모두 이 집합을 걸러내면 되도록 한 곳에서 만든다.
     *
     * <p>어느 쪽이 걸렸든 "상대"를 돌려주면 되므로 CASE 하나로 끝난다.
     */
    @Query("""
            select case when b.id.blockerId = :userId then b.id.blockedId
                        else b.id.blockerId end
            from Block b
            where b.id.blockerId = :userId or b.id.blockedId = :userId
            """)
    List<Long> findRelatedUserIds(@Param("userId") Long userId);

    /** 내가 차단한 사람만. 설정 화면의 차단 목록에 쓴다. */
    @Query("select b.id.blockedId from Block b where b.id.blockerId = :userId order by b.createdAt desc")
    List<Long> findBlockedUserIds(@Param("userId") Long userId);

    @Query("""
            select count(b) > 0 from Block b
            where (b.id.blockerId = :first and b.id.blockedId = :second)
               or (b.id.blockerId = :second and b.id.blockedId = :first)
            """)
    boolean existsBetween(@Param("first") Long first, @Param("second") Long second);

    boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    void deleteByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);
}
