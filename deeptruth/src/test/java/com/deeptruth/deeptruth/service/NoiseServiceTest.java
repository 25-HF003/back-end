package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseCreateRequestDTO;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoiseService 단위 테스트")
class NoiseServiceTest {

    @InjectMocks
    private NoiseService noiseService;

    @Mock
    private NoiseRepository noiseRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("존재하지 않는 사용자로 노이즈 이력 조회 시 예외 발생")
    void 존재하지_않는_사용자로_노이즈_이력_조회_실패() {
        // given
        Long nonExistentUserId = 999L;
        when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> noiseService.getUserNoiseHistory(nonExistentUserId));

        verify(userRepository).existsById(nonExistentUserId);
        verify(noiseRepository, never()).findAllByUser_UserId(anyLong());
    }

    @Test
    @DisplayName("사용자 노이즈 이력 조회 성공")
    void 사용자_노이즈_이력_조회_성공() {
        // given
        Long userId = 1L;
        List<Noise> mockNoiseList = Arrays.asList(
                테스트용_노이즈_생성(1L, userId),
                테스트용_노이즈_생성(2L, userId)
        );

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findAllByUser_UserId(userId))
                .thenReturn(mockNoiseList);

        // when
        List<NoiseDTO> result = noiseService.getUserNoiseHistory(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNoiseId()).isEqualTo(1L);
        assertThat(result.get(1).getNoiseId()).isEqualTo(2L);

        verify(userRepository).existsById(userId);
        verify(noiseRepository).findAllByUser_UserId(userId);
    }

    @Test
    @DisplayName("노이즈 이력이 없는 사용자 조회 시 빈 리스트 반환")
    void 노이즈_이력이_없는_사용자_조회() {
        // given
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findAllByUser_UserId(userId)).thenReturn(Arrays.asList());

        // when
        List<NoiseDTO> result = noiseService.getUserNoiseHistory(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userRepository).existsById(userId);
        verify(noiseRepository).findAllByUser_UserId(userId);
    }

    @Test
    @DisplayName("유효한 요청으로 노이즈 생성 성공")
    void 유효한_요청으로_노이즈_생성_성공() {
        // given
        Long userId = 1L;
        NoiseCreateRequestDTO request = NoiseCreateRequestDTO.builder()
                .originalFilePath("s3://test-bucket/original/test.jpg")
                .epsilon(0.1f)
                .build();

        User mockUser = 테스트용_사용자_생성(userId);
        Noise mockNoise = 테스트용_노이즈_생성(1L, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(noiseRepository.save(any(Noise.class))).thenReturn(mockNoise);

        // when
        NoiseDTO result = noiseService.createNoise(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNoiseId()).isEqualTo(1L);
        assertThat(result.getOriginalFilePath()).isEqualTo("s3://test-bucket/original/test.jpg");
        assertThat(result.getProcessedFilePath()).isEqualTo("s3://test-bucket/processed/test_noised.jpg");
        assertThat(result.getEpsilon()).isEqualTo(0.1f);

        verify(userRepository).findById(userId);
        verify(noiseRepository).save(any(Noise.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 노이즈 생성 시 예외 발생")
    void 존재하지_않는_사용자로_노이즈_생성_실패() {
        // given
        Long userId = 999L;
        NoiseCreateRequestDTO request = NoiseCreateRequestDTO.builder()
                .originalFilePath("s3://test-bucket/original/test.jpg")
                .epsilon(0.1f)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noiseService.createNoise(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다");

        verify(userRepository).findById(userId);
        verify(noiseRepository, never()).save(any(Noise.class));
    }

    @Test
    @DisplayName("빈 파일 경로로 노이즈 생성 시 예외 발생")
    void 빈_파일_경로로_노이즈_생성_실패() {
        // given
        Long userId = 1L;
        NoiseCreateRequestDTO request = NoiseCreateRequestDTO.builder()
                .originalFilePath("")
                .epsilon(0.1f)
                .build();

        // when & then
        assertThatThrownBy(() -> noiseService.createNoise(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본 파일 경로는 필수입니다");

        verify(userRepository, never()).findById(anyLong());
        verify(noiseRepository, never()).save(any(Noise.class));
    }

    @Test
    @DisplayName("잘못된 엡실론 값으로 노이즈 생성 시 예외 발생")
    void 잘못된_엡실론_값으로_노이즈_생성_실패() {
        // given
        Long userId = 1L;
        NoiseCreateRequestDTO request = NoiseCreateRequestDTO.builder()
                .originalFilePath("s3://test-bucket/original/test.jpg")
                .epsilon(-0.1f)
                .build();

        // when & then
        assertThatThrownBy(() -> noiseService.createNoise(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("엡실론 값은 0보다 큰 값이어야 합니다");

        verify(userRepository, never()).findById(anyLong());
        verify(noiseRepository, never()).save(any(Noise.class));
    }

    @Test
    @DisplayName("null 엡실론 값으로 노이즈 생성 시 예외 발생")
    void null_엡실론_값으로_노이즈_생성_실패() {
        // given
        Long userId = 1L;
        NoiseCreateRequestDTO request = NoiseCreateRequestDTO.builder()
                .originalFilePath("s3://test-bucket/original/test.jpg")
                .epsilon(null)
                .build();

        // when & then
        assertThatThrownBy(() -> noiseService.createNoise(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("엡실론 값은 0보다 큰 값이어야 합니다");

        verify(userRepository, never()).findById(anyLong());
        verify(noiseRepository, never()).save(any(Noise.class));
    }

    // 헬퍼 메소드
    // 테스트용 사용자 생성 (createNoise 테스트용)
    private User 테스트용_사용자_생성(Long userId) {
        return User.builder()
                .userId(userId)
                .loginId("testuser" + userId)
                .name("테스트유저" + userId)
                .nickname("테스터" + userId)
                .email("test" + userId + "@test.com")
                .build();
    }

    // 테스트용 노이즈 생성
    private Noise 테스트용_노이즈_생성(Long noiseId, Long userId) {
        User user = 테스트용_사용자_생성(userId);
        return Noise.builder()
                .noiseId(noiseId)
                .user(user)
                .originalFilePath("s3://test-bucket/original/test.jpg")
                .processedFilePath("s3://test-bucket/processed/test_noised.jpg")
                .epsilon(0.1f)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("노이즈 개별 조회 성공")
    void 노이즈_개별_조회_성공() {
        // given
        Long userId = 1L;
        Long noiseId = 1L;
        User mockUser = 테스트용_사용자_생성(userId);
        Noise mockNoise = 테스트용_노이즈_생성(noiseId, userId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findById(noiseId)).thenReturn(Optional.of(mockNoise));

        // when
        NoiseDTO result = noiseService.getNoiseById(userId, noiseId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNoiseId()).isEqualTo(noiseId);
        assertThat(result.getOriginalFilePath()).isEqualTo("s3://test-bucket/original/test.jpg");

        verify(userRepository).existsById(userId);
        verify(noiseRepository).findById(noiseId);
    }

    @Test
    @DisplayName("존재하지 않는 노이즈 조회 시 예외 발생")
    void 존재하지_않는_노이즈_조회_실패() {
        // given
        Long userId = 1L;
        Long noiseId = 999L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findById(noiseId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noiseService.getNoiseById(userId, noiseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("노이즈를 찾을 수 없습니다.");

        verify(userRepository).existsById(userId);
        verify(noiseRepository).findById(noiseId);
    }

    @Test
    @DisplayName("다른 사용자의 노이즈 조회 시 권한 예외 발생")
    void 권한_없는_노이즈_조회_실패() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long noiseId = 1L;

        Noise otherUserNoise = 테스트용_노이즈_생성(noiseId, otherUserId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findById(noiseId)).thenReturn(Optional.of(otherUserNoise));

        // when & then
        assertThatThrownBy(() -> noiseService.getNoiseById(userId, noiseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("접근 권한이 없습니다.");
    }


}