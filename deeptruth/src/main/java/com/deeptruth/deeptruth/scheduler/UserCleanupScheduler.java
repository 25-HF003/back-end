package com.deeptruth.deeptruth.scheduler;

import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final NoiseRepository noiseRepository;
    private final DeepfakeDetectionRepository deepfakeRepository;
    private final WatermarkRepository watermarkRepository;

    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시 실행
    @Transactional
    public void purgeDeletedUsers() {
        log.info("[INFO] 시작: 사용자 데이터 정리 작업");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // 탈퇴 후 30일 뒤 물리적 삭제
        int deletedUserCount = 0;
        long startTime = System.currentTimeMillis();

        try {
            // 30일 이전에 삭제된 사용자 조회
            List<User> usersToDelete = userRepository.findUsersForPermanentDeletion(cutoffDate);

            log.info("[INFO] 발견: {}명의 30일 경과 탈퇴 회원", usersToDelete.size());

            for (User user : usersToDelete) {
                try {
                    // 연관된 모든 데이터 삭제
                    deleteUserRelatedData(user);

                    // 사용자 완전 삭제
                    userRepository.deleteUserPermanently(user.getUserId());

                    deletedUserCount++;
                    log.info("[INFO] 완료: 회원 완전 삭제 - ID: {}, 로그인명: {}",
                            user.getUserId(), user.getLoginId());

                } catch (Exception e) {
                    log.warn("[WARN] 실패: 회원 삭제 처리 - ID: {}, 오류: {}",
                            user.getUserId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("[ERROR] 오류: 스케줄러 실행 중 예외 발생 - 원인: {}", e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[INFO] 데이터 정리 작업 - 처리된 회원: {}명, 소요시간: {}ms",
                    deletedUserCount, duration);
        }
    }

    // 사용자 관련 모든 데이터 삭제
    private void deleteUserRelatedData(User user) {
        log.debug("[DEBUG] 시작: 회원 연관 데이터 삭제 - ID: {}", user.getUserId());

        try {
            // 1. 딥페이크 탐지 기록 삭제
            int deepfakeCount = deepfakeRepository.deleteByUser(user);
            log.debug("[DEBUG] 삭제됨: 딥페이크 기록 {}건 - 회원 ID: {}", deepfakeCount, user.getUserId());

            // 2. 워터마크 기록 삭제
            int watermarkCount = watermarkRepository.deleteByUser(user);
            log.debug("[DEBUG] 삭제됨: 워터마크 기록 {}건 - 회원 ID: {}", watermarkCount, user.getUserId());

            // 3. 적대적 노이즈 기록 삭제
            int noiseCount = noiseRepository.deleteByUser(user);
            log.debug("[DEBUG] 삭제됨: 노이즈 기록 {}건 - 회원 ID: {}", noiseCount, user.getUserId());

            log.info("[INFO] 완료: 연관 데이터 삭제 - 회원 ID: {}, 딥페이크: {}건, 워터마크: {}건, 노이즈: {}건",
                    user.getUserId(), deepfakeCount, watermarkCount, noiseCount);

        } catch (Exception e) {
            log.warn("[WARN] 실패: 연관 데이터 삭제 중 오류 - 회원 ID: {}, 원인: {}",
                    user.getUserId(), e.getMessage(), e);
            throw e; // 상위로 전파
        }
    }
}
