package com.deeptruth.deeptruth.repository;

import com.deeptruth.deeptruth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    Optional<User> findByLoginId(String loginId);

    boolean existsByNickname(String nickname);
    boolean existsByUserId(Long userId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);

    /*
     30일 이상 지난 삭제 대상 사용자 조회
     @SQLRestriction을 우회하기 위해 nativeQuery 사용
     */
    @Query(value = "SELECT * FROM user WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffDate",
            nativeQuery = true)
    List<User> findUsersForPermanentDeletion(@Param("cutoffDate") LocalDateTime cutoffDate);

    // 회원 물리적 삭제
    @Modifying
    @Query(value = "DELETE FROM user WHERE user_id = :userId", nativeQuery = true)
    void deleteUserPermanently(@Param("userId") Long userId);
}
