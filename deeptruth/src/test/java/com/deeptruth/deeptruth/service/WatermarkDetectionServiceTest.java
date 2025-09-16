package com.deeptruth.deeptruth.service;
import com.deeptruth.deeptruth.base.dto.watermarkDetection.DetectResultDTO;
import com.deeptruth.deeptruth.base.dto.watermarkDetection.WatermarkDetectionFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import com.deeptruth.deeptruth.util.ImageHashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WatermarkDetectionServiceTest {
    @Mock private WatermarkRepository watermarkRepository;
    @Mock private WebClient webClient;
    @Mock private UserRepository userRepository;

    @Mock private WebClient.RequestBodyUriSpec uriSpec;
    @Mock private WebClient.RequestBodySpec bodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> headersSpec;

    @InjectMocks
    private WatermarkDetectionService service;

    @BeforeEach
    void setUp() {
        try {
            var f = WatermarkService.class.getDeclaredField("flaskServerUrl");
            f.setAccessible(true);
            f.set(service, "http://fake-flask.local");
        } catch (Exception ignored) {}
    }

    private static User user(long id) {
        return User.builder()
                .userId(id)
                .email("u@test.com")
                .loginId("login")
                .name("name")
                .nickname("nick")
                .password("pwd")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();
    }

    private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private void mockWebClientOk(WatermarkDetectionFlaskResponseDTO flaskDto) {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(eq(MediaType.MULTIPART_FORM_DATA))).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.exchangeToMono(any())).thenReturn(Mono.just(flaskDto));
    }

    private static byte[] tinyPng() {
        try {
            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0x00FF00); // 아무 색 1픽셀
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("detect 성공: sha/nsha 미스 → pHash 근사 매칭 → Flask OK")
    void detect_success_phash() {
        // given
        long uid = 7L;
        when(userRepository.findById(uid)).thenReturn(Optional.of(user(uid)));

        byte[] bytes = tinyPng();
        MockMultipartFile mf = file("sample.png", "image/png", bytes);

        // sha/nsha는 미스
        when(watermarkRepository.findFirstBySha256(anyString()))
                .thenReturn(Optional.empty());
        when(watermarkRepository.findFirstByNormalizedSha256(anyString()))
                .thenReturn(Optional.empty());

        // pHash 근사 매칭: 테스트에서 실제 pHash 값을 계산해서 동일하게 맞춰 거리 0으로
        long p = ImageHashUtils.pHash(bytes);
        Watermark near = Watermark.builder()
                .watermarkId(1L)
                .artifactId("art-123")
                .message("ABCD")
                .phash(p)
                .build();
        when(watermarkRepository.findNearestByPhash(anyLong()))
                .thenReturn(near);

        // Flask OK 응답
        WatermarkDetectionFlaskResponseDTO flask = new WatermarkDetectionFlaskResponseDTO();
        flask.setBit_accuracy(0.93D);
        flask.setDetected_at("2025-01-01T00:00:00Z");
        flask.setImage_base64(null);
        flask.setBasename("sample.png");
        mockWebClientOk(flask);

        // when
        DetectResultDTO result = service.detect(uid, mf, "task-1");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getArtifactId()).isEqualTo("art-123");
        assertThat(result.getMatchMethod()).isEqualTo("PHASH");
        assertThat(result.getPhashDistance()).isEqualTo(0);
        assertThat(result.getBitAccuracy()).isEqualTo(0.93D);
        assertThat(result.getDetectedAt()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(result.getTaskId()).isEqualTo("task-1");

        verify(userRepository).findById(uid);
        verify(watermarkRepository).findFirstBySha256(anyString());
        verify(watermarkRepository).findFirstByNormalizedSha256(anyString());
        verify(watermarkRepository).findNearestByPhash(anyLong());
        verify(webClient).post();
    }


    @Test
    @DisplayName("detect 실패: 사용자 없음 → UserNotFoundException")
    void detect_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        MockMultipartFile mf = file("a.png", "image/png", new byte[]{1});
        assertThatThrownBy(() -> service.detect(99L, mf, "t"))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(watermarkRepository);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("detect 실패: 빈 파일 → FileEmptyException")
    void detect_emptyFile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile empty = file("a.png", "image/png", new byte[]{});
        assertThatThrownBy(() -> service.detect(1L, empty, "t"))
                .isInstanceOf(FileEmptyException.class);
        verifyNoInteractions(watermarkRepository);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("detect 실패: 파일명에 확장자 없음 → InvalidFilenameException")
    void detect_invalidFilename() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile mf = file("invalid", "image/png", new byte[]{1});
        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(InvalidFilenameException.class);
        verifyNoInteractions(watermarkRepository);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("detect 실패: pHash 근처 아티팩트 없음 → ArtifactNotFoundException")
    void detect_noSimilarArtifact() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        byte[] bytes = tinyPng();
        MockMultipartFile mf = file("a.png", "image/png", bytes);

        when(watermarkRepository.findFirstBySha256(anyString())).thenReturn(Optional.empty());
        when(watermarkRepository.findFirstByNormalizedSha256(anyString())).thenReturn(Optional.empty());
        when(watermarkRepository.findNearestByPhash(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(ArtifactNotFoundException.class);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("detect 실패: pHash 거리 임계 초과 → SimilarityThresholdExceededException")
    void detect_similarityThresholdExceeded() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        byte[] bytes = tinyPng();
        MockMultipartFile mf = file("a.png", "image/png", bytes);

        when(watermarkRepository.findFirstBySha256(anyString())).thenReturn(Optional.empty());
        when(watermarkRepository.findFirstByNormalizedSha256(anyString())).thenReturn(Optional.empty());

        long p = ImageHashUtils.pHash(bytes);
        // p와 충분히 떨어진 값(다른 비트 많이 뒤집기). 간단히 보수(~p) 사용 -> 해밍거리는 보통 64에 근접
        Watermark far = Watermark.builder()
                .artifactId("far")
                .message("ABCD")
                .phash(~p)
                .build();
        when(watermarkRepository.findNearestByPhash(anyLong())).thenReturn(far);

        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(SimilarityThresholdExceededException.class);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("detect 실패: sha256 정확 매칭되지만 message 없음 → DataMappingException")
    void detect_messageMissing_onShaHit() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile mf = file("a.png", "image/png", new byte[]{1,2,3});

        Watermark hit = Watermark.builder()
                .artifactId("hit")
                .message(null) // 메시지 없음
                .build();
        when(watermarkRepository.findFirstBySha256(anyString())).thenReturn(Optional.of(hit));

        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("메시지가 없습니다");
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("detect 실패: Flask exchangeToMono 비정상(예: 4xx/5xx 경로에서 IllegalStateException) → ExternalServiceException(Flask invocation failed)")
    void detect_flaskNon2xxFlow() {
        // sha/nsha 미스 → pHash 매칭까지 진행되게 목 구성
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        byte[] bytes = tinyPng();
        MockMultipartFile mf = file("a.png", "image/png", bytes);

        when(watermarkRepository.findFirstBySha256(anyString())).thenReturn(Optional.empty());
        when(watermarkRepository.findFirstByNormalizedSha256(anyString())).thenReturn(Optional.empty());
        long p = ImageHashUtils.pHash(bytes);
        Watermark near = Watermark.builder().artifactId("art").message("ABCD").phash(p).build();
        when(watermarkRepository.findNearestByPhash(anyLong())).thenReturn(near);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(eq(MediaType.MULTIPART_FORM_DATA))).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        // exchangeToMono 내부에서 4xx/5xx면 IllegalStateException 으로 Mono.error -> 최종적으로 ExternalServiceException("Flask invocation failed")
        when(headersSpec.exchangeToMono(any())).thenReturn(Mono.error(new IllegalStateException("4xx")));

        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Flask invocation failed");
    }

    @Test
    @DisplayName("detect 실패: Flask 응답 null → ExternalServiceException")
    void detect_flaskNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        byte[] bytes = tinyPng();
        MockMultipartFile mf = file("a.png", "image/png", bytes);

        when(watermarkRepository.findFirstBySha256(anyString())).thenReturn(Optional.empty());
        when(watermarkRepository.findFirstByNormalizedSha256(anyString())).thenReturn(Optional.empty());
        long p = ImageHashUtils.pHash(bytes);
        Watermark near = Watermark.builder().artifactId("art").message("ABCD").phash(p).build();
        when(watermarkRepository.findNearestByPhash(anyLong())).thenReturn(near);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(eq(MediaType.MULTIPART_FORM_DATA))).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.exchangeToMono(any())).thenReturn(Mono.justOrEmpty(null)); // null

        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("응답이 비어");
    }

    @Test
    @DisplayName("detect 실패: Flask 필드 누락(bit_accuracy/detected_at) → DataMappingException")
    void detect_flaskMissingFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        byte[] bytes = tinyPng();
        MockMultipartFile mf = file("a.png", "image/png", bytes);

        when(watermarkRepository.findFirstBySha256(anyString())).thenReturn(Optional.empty());
        when(watermarkRepository.findFirstByNormalizedSha256(anyString())).thenReturn(Optional.empty());
        long p = ImageHashUtils.pHash(bytes);
        Watermark near = Watermark.builder().artifactId("art").message("ABCD").phash(p).build();
        when(watermarkRepository.findNearestByPhash(anyLong())).thenReturn(near);

        WatermarkDetectionFlaskResponseDTO flask = new WatermarkDetectionFlaskResponseDTO();
        // bit_accuracy / detected_at 누락
        mockWebClientOk(flask);

        assertThatThrownBy(() -> service.detect(1L, mf, "t"))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("bit_accuracy");
    }
}