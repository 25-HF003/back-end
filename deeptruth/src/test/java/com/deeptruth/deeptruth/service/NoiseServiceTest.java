package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.base.dto.noise.NoiseFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.ImageDecodingException;
import com.deeptruth.deeptruth.base.exception.NoiseNotFoundException;
import com.deeptruth.deeptruth.base.exception.UserNotFoundException;
import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoiseService 단위 테스트")
@Transactional
class NoiseServiceTest {

    @InjectMocks
    private NoiseService noiseService;

    @Mock
    private NoiseRepository noiseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AmazonS3Service amazonS3Service;

    private User testUser;
    private NoiseFlaskResponseDTO testFlaskResponse;
    private Noise testNoise;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .build();

        testFlaskResponse = NoiseFlaskResponseDTO.builder()
                .originalFilePath("https://s3.amazonaws.com/original.jpg")
                .processedFilePath("https://s3.amazonaws.com/processed.jpg")
                .epsilon(0.05F)
                .attackSuccess(true)
                .originalPrediction("High Renaissance")
                .adversarialPrediction("Surrealism")
                .originalConfidence("0.543")
                .adversarialConfidence("0.143")
                .confidenceDrop("40.0%")
                .message("적대적 노이즈 생성 완료")
                .build();

        testNoise = Noise.builder()
                .noiseId(1L)
                .user(testUser)
                .originalFilePath("https://s3.amazonaws.com/original.jpg")
                .processedFilePath("https://s3.amazonaws.com/processed.jpg")
                .epsilon(0.05F)
                .attackSuccess(true)
                .originalPrediction("High Renaissance")
                .adversarialPrediction("Surrealism")
                .build();
    }

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
    @DisplayName("적대적 노이즈 생성 성공 테스트")
    void 적대적노이즈생성_성공테스트() {
        // given: Flask 응답 DTO와 사용자 정보
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(noiseRepository.save(any(Noise.class))).thenReturn(testNoise);

        // when
        NoiseDTO result = noiseService.createNoise(1L, testFlaskResponse);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilePath()).isEqualTo("https://s3.amazonaws.com/original.jpg");
        assertThat(result.getProcessedFilePath()).isEqualTo("https://s3.amazonaws.com/processed.jpg");
        assertThat(result.getAttackSuccess()).isTrue();
        assertThat(result.getOriginalPrediction()).isEqualTo("High Renaissance");
        assertThat(result.getAdversarialPrediction()).isEqualTo("Surrealism");

        verify(userRepository).findById(1L);
        verify(noiseRepository).save(any(Noise.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 노이즈 생성 시 예외 발생")
    void 적대적노이즈생성_사용자없음_예외발생() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noiseService.createNoise(999L, testFlaskResponse))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(999L);
        verify(noiseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Base64 이미지 S3 업로드 성공 테스트")
    void Base64이미지_S3업로드_성공테스트() {
        // given
        String validBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
        String expectedS3Url = "https://s3.amazonaws.com/test-image.jpg";

        when(amazonS3Service.uploadBase64Image(any(), anyString()))
                .thenReturn(expectedS3Url);

        // when
        String result = noiseService.uploadBase64ImageToS3(validBase64, 1L, "test");

        // then
        assertThat(result).isEqualTo(expectedS3Url);
        verify(amazonS3Service).uploadBase64Image(any(), contains("noise/1/test/"));
    }

    @Test
    @DisplayName("잘못된 Base64 데이터로 인한 예외 테스트")
    void Base64이미지업로드_잘못된데이터_예외발생() {
        // given
        String invalidBase64 = "invalid-base64-data!!!";

        // when & then
        assertThatThrownBy(() -> noiseService.uploadBase64ImageToS3(invalidBase64, 1L, "test"))
                .isInstanceOf(ImageDecodingException.class)
                .hasMessageContaining("Invalid Base64 characters detected");

        verify(amazonS3Service, never()).uploadBase64Image(any(), any());
    }

    @Test
    @DisplayName("빈 Base64 문자열로 인한 예외 테스트")
    void Base64이미지업로드_빈문자열_예외발생() {
        // given
        String emptyBase64 = "";

        // when & then
        assertThatThrownBy(() -> noiseService.uploadBase64ImageToS3(emptyBase64, 1L, "test"))
                .isInstanceOf(ImageDecodingException.class)
                .hasMessageContaining("empty string");
    }

    @Test
    @DisplayName("사용자별 노이즈 목록 조회 테스트")
    void 사용자별노이즈목록조회_성공테스트() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Noise> noisePage = new PageImpl<>(List.of(testNoise));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(noiseRepository.findByUser_UserId(1L, pageable)).thenReturn(noisePage);

        // when
        Page<NoiseDTO> result = noiseService.getAllResult(1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getNoiseId()).isEqualTo(1L);

        verify(userRepository).findById(1L);
        verify(noiseRepository).findByUser_UserId(1L, pageable);
    }

    @Test
    @DisplayName("존재하지 않는 노이즈 조회 시 예외 테스트")
    void 개별노이즈조회_존재하지않음_예외발생() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(noiseRepository.findByNoiseIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noiseService.getSingleResult(1L, 999L))
                .isInstanceOf(NoiseNotFoundException.class);

        verify(userRepository).findById(1L);
        verify(noiseRepository).findByNoiseIdAndUser(999L, testUser);
    }

    @Test
    @DisplayName("노이즈 삭제 성공 테스트")
    void 노이즈삭제_성공테스트() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(noiseRepository.deleteByNoiseIdAndUser(1L, testUser)).thenReturn(1);

        // when & then
        assertThatNoException().isThrownBy(() -> noiseService.deleteResult(1L, 1L));

        verify(userRepository).findById(1L);
        verify(noiseRepository).deleteByNoiseIdAndUser(1L, testUser);
    }

    @Test
    @DisplayName("존재하지 않는 노이즈 삭제 시 예외 테스트")
    void 노이즈삭제_존재하지않음_예외발생() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(noiseRepository.deleteByNoiseIdAndUser(999L, testUser)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> noiseService.deleteResult(1L, 999L))
                .isInstanceOf(NoiseNotFoundException.class);
    }
/*
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

    @Test
    @DisplayName("노이즈 삭제 성공")
    void 노이즈_삭제_성공() {
        // given
        Long userId = 1L;
        Long noiseId = 1L;
        Noise mockNoise = 테스트용_노이즈_생성(noiseId, userId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findById(noiseId)).thenReturn(Optional.of(mockNoise));
        doNothing().when(noiseRepository).delete(mockNoise);

        // when
        noiseService.deleteNoise(userId, noiseId);

        // then
        verify(userRepository).existsById(userId);
        verify(noiseRepository).findById(noiseId);
        verify(noiseRepository).delete(mockNoise);
    }

    @Test
    @DisplayName("존재하지 않는 노이즈 삭제 시 예외 발생")
    void 존재하지_않는_노이즈_삭제_실패() {
        // given
        Long userId = 1L;
        Long noiseId = 999L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findById(noiseId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noiseService.deleteNoise(userId, noiseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("노이즈를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("다른 사용자의 노이즈 삭제 시 권한 예외 발생")
    void 권한_없는_노이즈_삭제_실패() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long noiseId = 1L;

        Noise otherUserNoise = 테스트용_노이즈_생성(noiseId, otherUserId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(noiseRepository.findById(noiseId)).thenReturn(Optional.of(otherUserNoise));

        // when & then
        assertThatThrownBy(() -> noiseService.deleteNoise(userId, noiseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("접근 권한이 없습니다.");
    }
*/

}