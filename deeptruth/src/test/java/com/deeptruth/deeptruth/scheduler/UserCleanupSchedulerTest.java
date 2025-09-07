package com.deeptruth.deeptruth.scheduler;

import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserCleanupSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoiseRepository noiseRepository;

    @Mock
    private DeepfakeDetectionRepository deepfakeRepository;

    @Mock
    private WatermarkRepository watermarkRepository;

    @InjectMocks
    private UserCleanupScheduler scheduler;

    @Test
    @DisplayName("30일 이상 경과한 탈퇴 회원과 관련 데이터가 완전히 삭제되는지 검증")
    void 삭제된지_30일_이상_경과한_회원과_관련_데이터_정리_성공_테스트() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getUserId()).thenReturn(1L);
        when(testUser.getLoginId()).thenReturn("testuser123");

        List<User> usersToDelete = Arrays.asList(testUser);
        when(userRepository.findUsersForPermanentDeletion(any(LocalDateTime.class)))
                .thenReturn(usersToDelete);

        // any()를 사용해서 어떤 User 객체든 처리 가능하도록 설정
        when(deepfakeRepository.deleteByUser(any(User.class))).thenReturn(2);
        when(watermarkRepository.deleteByUser(any(User.class))).thenReturn(1);
        when(noiseRepository.deleteByUser(any(User.class))).thenReturn(3);

        // When
        scheduler.purgeDeletedUsers();

        // Then
        verify(userRepository, times(1)).findUsersForPermanentDeletion(any(LocalDateTime.class));
        verify(deepfakeRepository, times(1)).deleteByUser(any(User.class));
        verify(watermarkRepository, times(1)).deleteByUser(any(User.class));
        verify(noiseRepository, times(1)).deleteByUser(any(User.class));
        verify(userRepository, times(1)).deleteUserPermanently(1L);
    }

    @Test
    @DisplayName("30일 미만인 경우 삭제 대상이 없을 때 정상 동작")
    void 삭제_대상_없을때_정상_동작_테스트() {
        // Given
        when(userRepository.findUsersForPermanentDeletion(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        scheduler.purgeDeletedUsers();

        // Then
        verify(userRepository, times(1)).findUsersForPermanentDeletion(any(LocalDateTime.class));
        verify(deepfakeRepository, never()).deleteByUser(any());
        verify(watermarkRepository, never()).deleteByUser(any());
        verify(noiseRepository, never()).deleteByUser(any());
        verify(userRepository, never()).deleteUserPermanently(anyLong());
    }

    @Test
    @DisplayName("여러 사용자 삭제 시 모든 사용자에 대해 정리 작업 수행")
    void 여러_회원_동시_정리_테스트() {
        // Given
        User user1 = mock(User.class);
        User user2 = mock(User.class);
        when(user1.getUserId()).thenReturn(1L);
        when(user2.getUserId()).thenReturn(2L);
        when(user1.getLoginId()).thenReturn("user1");
        when(user2.getLoginId()).thenReturn("user2");

        List<User> usersToDelete = Arrays.asList(user1, user2);
        when(userRepository.findUsersForPermanentDeletion(any(LocalDateTime.class)))
                .thenReturn(usersToDelete);

        when(deepfakeRepository.deleteByUser(any(User.class))).thenReturn(1);
        when(watermarkRepository.deleteByUser(any(User.class))).thenReturn(1);
        when(noiseRepository.deleteByUser(any(User.class))).thenReturn(1);

        // When
        scheduler.purgeDeletedUsers();

        // Then
        verify(deepfakeRepository, times(2)).deleteByUser(any(User.class));
        verify(watermarkRepository, times(2)).deleteByUser(any(User.class));
        verify(noiseRepository, times(2)).deleteByUser(any(User.class));
        verify(userRepository, times(1)).deleteUserPermanently(1L);
        verify(userRepository, times(1)).deleteUserPermanently(2L);
    }

    @Test
    @DisplayName("관련 데이터 삭제 중 예외 발생 시 해당 사용자 처리는 실패하지만 다른 사용자는 계속 처리")
    void 개별_회원_삭제_실패시_다른_회원_처리_계속_진행_테스트() {
        // Given
        User user1 = mock(User.class);
        User user2 = mock(User.class);
        when(user1.getUserId()).thenReturn(1L);
        when(user2.getUserId()).thenReturn(2L);
        when(user1.getLoginId()).thenReturn("user1");
        when(user2.getLoginId()).thenReturn("user2");

        List<User> usersToDelete = Arrays.asList(user1, user2);
        when(userRepository.findUsersForPermanentDeletion(any(LocalDateTime.class)))
                .thenReturn(usersToDelete);

        // user1 처리 시 예외 발생
        when(noiseRepository.deleteByUser(any(User.class)))
                .thenThrow(new RuntimeException("DB 연결 오류"))
                .thenReturn(1); // 두 번째 호출부터는 정상 처리

        when(deepfakeRepository.deleteByUser(any(User.class))).thenReturn(1);
        when(watermarkRepository.deleteByUser(any(User.class))).thenReturn(1);

        // When
        scheduler.purgeDeletedUsers();

        // Then
        // user1은 예외로 인해 완전 삭제되지 않음
        verify(userRepository, never()).deleteUserPermanently(1L);

        // user2는 정상적으로 삭제됨
        verify(userRepository, times(1)).deleteUserPermanently(2L);
    }

    @Test
    @DisplayName("사용자 조회 자체에서 예외 발생 시 스케줄러가 중단되지 않음")
    void 사용자_조회_예외시_스케줄러_견고성_테스트() {
        // Given
        when(userRepository.findUsersForPermanentDeletion(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("데이터베이스 연결 실패"));

        // When
        scheduler.purgeDeletedUsers();

        // Then
        verify(userRepository, times(1)).findUsersForPermanentDeletion(any(LocalDateTime.class));
        verify(deepfakeRepository, never()).deleteByUser(any());
        verify(watermarkRepository, never()).deleteByUser(any());
        verify(noiseRepository, never()).deleteByUser(any());
        verify(userRepository, never()).deleteUserPermanently(anyLong());
    }

    @Test
    @DisplayName("물리적 사용자 삭제 실패 시에도 스케줄러는 계속 동작")
    void 물리적_사용자_삭제_실패시_계속_동작_테스트() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getUserId()).thenReturn(1L);
        when(testUser.getLoginId()).thenReturn("testuser123");

        List<User> usersToDelete = Arrays.asList(testUser);
        when(userRepository.findUsersForPermanentDeletion(any(LocalDateTime.class)))
                .thenReturn(usersToDelete);

        when(deepfakeRepository.deleteByUser(any(User.class))).thenReturn(1);
        when(watermarkRepository.deleteByUser(any(User.class))).thenReturn(1);
        when(noiseRepository.deleteByUser(any(User.class))).thenReturn(1);

        // 물리적 사용자 삭제에서 예외 발생
        doThrow(new RuntimeException("제약조건 위반"))
                .when(userRepository).deleteUserPermanently(1L);

        // When
        scheduler.purgeDeletedUsers();

        // Then
        verify(deepfakeRepository, times(1)).deleteByUser(any(User.class));
        verify(watermarkRepository, times(1)).deleteByUser(any(User.class));
        verify(noiseRepository, times(1)).deleteByUser(any(User.class));
        verify(userRepository, times(1)).deleteUserPermanently(1L);
    }
}
