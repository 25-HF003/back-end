package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.DeepfakeDetector;
import com.deeptruth.deeptruth.base.Enum.DeepfakeMode;
import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.base.dto.deepfake.*;
import com.deeptruth.deeptruth.base.exception.*;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.DeepfakeDetectionRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeepfakeDetectionServiceTest {

    @InjectMocks private DeepfakeDetectionService service;

    @Mock private UserRepository userRepository;
    @Mock private DeepfakeDetectionRepository deepfakeDetectionRepository;
    @Mock private AmazonS3Service amazonS3Service;
    @Mock private DeepfakeViewAssembler assembler;
    @Mock private WebClient webClient;

    @Mock private WebClient.RequestBodyUriSpec uriSpec;
    @Mock private WebClient.RequestBodySpec bodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        try {
            var f = WatermarkService.class.getDeclaredField("flaskServerUrl");
            f.setAccessible(true);
            f.set(service, "http://fake-flask.local");
        } catch (Exception ignored) {}
    }


    private void mockWebClientReturning(FlaskResponseDTO dto) {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(eq(MediaType.MULTIPART_FORM_DATA))).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.MultipartInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(FlaskResponseDTO.class))).thenReturn(Mono.justOrEmpty(dto));
    }

    private static MockMultipartFile mockFile(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private static User user(long id) {
        return User.builder()
                .userId(id)
                .email("u@test.com")
                .loginId("login")
                .name("name")
                .nickname("nick")
                .password("pwd")
                .createdAt(LocalDate.now())
                .build();
    }

    private static FlaskResponseDTO minimalFlaskResponse() {
        FlaskResponseDTO r = new FlaskResponseDTO();
        r.setTaskId("tid-123");
        r.setImageUrl("s3://after-upload.jpg");
        r.setResult(DeepfakeResult.FAKE); // or REAL
        r.setScoreWeighted(0.77f);
        r.setThresholdTau(0.5f);
        r.setFrameVoteRatio(0.9f);
        r.setAverageConfidence(0.8f);
        r.setMedianConfidence(0.81f);
        r.setMaxConfidence(0.95f);
        r.setVarianceConfidence(0.02f);
        r.setFramesProcessed(123);
        r.setProcessingTimeSec(1.23f);
        r.setMode(DeepfakeMode.DEFAULT.name());
        r.setDetector(DeepfakeDetector.DLIB.name());
        r.setUseTta(true);
        r.setUseIllum(false);
        r.setMinFace(64);
        r.setSampleCount(8);
        r.setSmoothWindow(3);

        Speed sp = new Speed();
        sp.setMsPerSample(12.3f);
        sp.setTargetFps(20.0f);
        sp.setMaxLatencyMs(200.0f);
        sp.setSpeedOk(true);
        sp.setFpsProcessed(18.7f);
        r.setSpeed(sp);

        StabilityEvidence se = new StabilityEvidence();
        se.setTemporalDeltaMean(0.1f);
        se.setTemporalDeltaStd(0.02f);
        se.setTtaMean(0.3f);
        se.setTtaStd(0.05f);
        r.setStabilityEvidence(se);

        Timeseries ts = new Timeseries();
        ts.setPerFrameConf(Arrays.asList(0.1f, 0.5f, 0.9f));
        ts.setVmin(0.0f);
        ts.setVmax(1.0f);
        r.setTimeseries(ts);

        return r;
    }

    @Test
    @DisplayName("createDetection 성공: Flask 응답 매핑, base64 업로드, bullet 계산, 저장까지")
    void createDetection_success() throws IOException {
        long uid = 10L;
        when(userRepository.findById(uid)).thenReturn(Optional.of(user(uid)));

        // 파일
        byte[] bytes = new byte[]{1, 2, 3};
        MockMultipartFile file = mockFile("video.mp4", "video/mp4", bytes);

        FlaskResponseDTO flask = minimalFlaskResponse();
        flask.setBase64Url(Base64.getEncoder().encodeToString(new byte[]{9, 9, 9}));
        flask.setImageUrl(null);
        mockWebClientReturning(flask);

        // S3 업로드 (base64 이미지)
        when(amazonS3Service.uploadBase64Image(any(InputStream.class), argThat(k -> k.startsWith("deepfake/" + uid + "/"))))
                .thenReturn("https://s3.example/df/" + uid + "/thumb.jpg");

        when(deepfakeDetectionRepository.save(any(DeepfakeDetection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // bullets
        List<BulletDTO> stability = List.of(
                BulletDTO.builder()
                        .key("a")
                        .label("b")
                        .value(0.7f)
                        .bands(null)
                        .direction("higher")
                        .unit(null)
                        .build()
        );

        List<BulletDTO> speed = List.of(
                BulletDTO.builder()
                        .key("c")
                        .label("d")
                        .value(0.8f)
                        .bands(null)
                        .direction("higher")
                        .unit(null)
                        .build()
        );

        when(assembler.makeStabilityBullets(any())).thenReturn(stability);
        when(assembler.makeSpeedBullets(any())).thenReturn(speed);

        Map<String, String> form = new HashMap<>();
        form.put("mode", "DEFAULT");
        form.put("detector", "DLIB");
        form.put("taskId", "tid-123");
        form.put("use_tta", "true");

        DeepfakeDetectionDTO dto = service.createDetection(uid, file, form);

        assertThat(dto).isNotNull();
        assertThat(dto.getTaskId()).isEqualTo("tid-123");
        assertThat(dto.getResult()).isEqualTo(DeepfakeResult.FAKE);
        assertThat(dto.getFilePath()).isEqualTo("https://s3.example/df/" + uid + "/thumb.jpg");

        verify(webClient).post();
        verify(amazonS3Service).uploadBase64Image(any(InputStream.class), anyString());
        verify(deepfakeDetectionRepository).save(any(DeepfakeDetection.class));
        verify(assembler).makeStabilityBullets(any());
        verify(assembler).makeSpeedBullets(any());
    }

    @Test
    @DisplayName("createDetection 실패: 사용자 없음")
    void createDetection_userNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> service.createDetection(999L, file, Map.of()))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(webClient, amazonS3Service, deepfakeDetectionRepository, assembler);
    }

    @Test
    @DisplayName("createDetection 실패: 파일 비었음")
    void createDetection_fileEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(FileEmptyException.class);
    }


    @Test
    @DisplayName("createDetection 실패: 지원하지 않는 미디어 타입")
    void createDetection_unsupportedMediaType() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.bin", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    @DisplayName("createDetection 실패: Flask 응답 null")
    void createDetection_flaskNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        mockWebClientReturning(null);

        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("createDetection 실패: Flask HTTP 에러(4xx/5xx)")
    void createDetection_flaskHttpError() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.MultipartInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FlaskResponseDTO.class))
                .thenThrow(new org.springframework.web.reactive.function.client.WebClientResponseException(
                        "boom", 500, "Internal", null, null, null));

        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Flask HTTP error");
    }

    @Test
    @DisplayName("createDetection 실패: Flask 요청 실패(연결/타임아웃)")
    void createDetection_flaskRequestError() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.MultipartInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        WebClientRequestException connError = new WebClientRequestException(
                new IOException("conn fail"),
                HttpMethod.POST,
                URI.create("http://fake-flask.local/predict"),
                HttpHeaders.EMPTY
        );
        when(responseSpec.bodyToMono(eq(FlaskResponseDTO.class)))
                .thenReturn(Mono.error(connError));
        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("request failed");
    }

    @Test
    @DisplayName("createDetection 실패: 필수 필드 누락 → DataMappingException")
    void createDetection_missingRequiredFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        FlaskResponseDTO bad = new FlaskResponseDTO(); // taskId/imageUrl/result 없음
        mockWebClientReturning(bad);

        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(DataMappingException.class);
    }

    @Test
    @DisplayName("createDetection 실패: 잘못된 enum 값 → InvalidEnumValueException")
    void createDetection_invalidEnum() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        FlaskResponseDTO r = minimalFlaskResponse();
        r.setMode("NOPE"); // 잘못된 모드
        mockWebClientReturning(r);

        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(InvalidEnumValueException.class);
    }

    @Test
    @DisplayName("createDetection 실패: bullet assembler 가 null 반환 → DataMappingException")
    void createDetection_bulletsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        MockMultipartFile file = mockFile("x.png", "image/png", new byte[]{1});

        FlaskResponseDTO r = minimalFlaskResponse();
        mockWebClientReturning(r);

        List<BulletDTO> speed = List.of(
                BulletDTO.builder()
                        .key("c")
                        .label("d")
                        .value(0.8f)
                        .bands(null)
                        .direction("higher")
                        .unit(null)
                        .build()
        );

        when(assembler.makeStabilityBullets(any())).thenReturn(null);
        when(assembler.makeSpeedBullets(any())).thenReturn(speed);

        assertThatThrownBy(() -> service.createDetection(1L, file, Map.of()))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("bullet");
    }

    // ========== getAllResult ==========

    @Test
    @DisplayName("getAllResult 성공: 페이지 결과 반환")
    void getAllResult_success() {
        long uid = 3L;
        when(userRepository.findById(uid)).thenReturn(Optional.of(user(uid)));

        DeepfakeDetection entity = new DeepfakeDetection();
        entity.setDeepfakeDetectionId(1L);
        entity.setTaskId("t1");
        entity.setResult(DeepfakeResult.REAL);
        Page<DeepfakeDetection> page = new PageImpl<>(List.of(entity), PageRequest.of(0,10), 1);

        when(deepfakeDetectionRepository.findByUser_UserId(eq(uid), any(Pageable.class)))
                .thenReturn(page);

        Page<DeepfakeDetectionListDTO> out = service.getAllResult(uid, PageRequest.of(0,10));
        assertThat(out.getTotalElements()).isEqualTo(1);
        assertThat(out.getContent().get(0).getTaskId()).isEqualTo("t1");
    }

    @Test
    @DisplayName("getAllResult 실패: pageable null")
    void getAllResult_pageableNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        assertThatThrownBy(() -> service.getAllResult(1L, null))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("pageable");
    }

    // ========== getSingleResult ==========

    @Test
    @DisplayName("getSingleResult 성공")
    void getSingleResult_success() {
        long uid = 4L;
        when(userRepository.findById(uid)).thenReturn(Optional.of(user(uid)));

        DeepfakeDetection entity = new DeepfakeDetection();
        entity.setDeepfakeDetectionId(10L);
        entity.setTaskId("tid");
        entity.setResult(DeepfakeResult.REAL);

        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(eq(10L), any(User.class)))
                .thenReturn(Optional.of(entity));

        List<BulletDTO> stability = List.of(
                BulletDTO.builder()
                        .key("a")
                        .label("b")
                        .value(0.7f)
                        .bands(null)
                        .direction("higher")
                        .unit(null)          
                        .build()
        );

        List<BulletDTO> speed = List.of(
                BulletDTO.builder()
                        .key("c")
                        .label("d")
                        .value(0.8f)
                        .bands(null)
                        .direction("higher")
                        .unit(null)
                        .build()
        );

        when(assembler.makeStabilityBullets(entity))
                .thenReturn(stability);
        when(assembler.makeSpeedBullets(entity))
                .thenReturn(speed);

        DeepfakeDetectionDTO dto = service.getSingleResult(uid, 10L);
        assertThat(dto.getTaskId()).isEqualTo("tid");
        assertThat(dto.getResult()).isEqualTo(DeepfakeResult.REAL);
    }

    @Test
    @DisplayName("getSingleResult 실패: 사용자 없음")
    void getSingleResult_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSingleResult(1L, 10L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getSingleResult 실패: 엔티티 없음")
    void getSingleResult_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(eq(10L), any(User.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSingleResult(1L, 10L))
                .isInstanceOf(DetectionNotFoundException.class);
    }

    @Test
    @DisplayName("getSingleResult 실패: bullet assembler 가 null 반환 → DataCorruptionException")
    void getSingleResult_bulletsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        DeepfakeDetection entity = new DeepfakeDetection();
        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(eq(10L), any(User.class)))
                .thenReturn(Optional.of(entity));

        List<BulletDTO> speed = List.of(
                BulletDTO.builder()
                        .key("c")
                        .label("d")
                        .value(0.8f)
                        .bands(null)
                        .direction("higher")
                        .unit(null)
                        .build()
        );

        when(assembler.makeStabilityBullets(entity)).thenReturn(null);
        when(assembler.makeSpeedBullets(entity)).thenReturn(speed);

        assertThatThrownBy(() -> service.getSingleResult(1L, 10L))
                .isInstanceOf(DataCorruptionException.class)
                .hasMessageContaining("null list");
    }

    // ========== deleteResult ==========

    @Test
    @DisplayName("deleteResult 성공")
    void deleteResult_success() {
        long uid = 8L;
        when(userRepository.findById(uid)).thenReturn(Optional.of(user(uid)));
        DeepfakeDetection entity = new DeepfakeDetection();
        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(eq(100L), any(User.class)))
                .thenReturn(Optional.of(entity));
        when(deepfakeDetectionRepository.deleteByDeepfakeDetectionIdAndUser(eq(100L), any(User.class)))
                .thenReturn(1);

        service.deleteResult(uid, 100L);

        verify(deepfakeDetectionRepository).deleteByDeepfakeDetectionIdAndUser(eq(100L), any(User.class));
    }

    @Test
    @DisplayName("deleteResult 실패: 사용자 없음")
    void deleteResult_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteResult(1L, 100L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("deleteResult 실패: 대상 없음(조회 단계)")
    void deleteResult_notFoundOnLookup() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(eq(100L), any(User.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteResult(1L, 100L))
                .isInstanceOf(DetectionNotFoundException.class);
    }

    @Test
    @DisplayName("deleteResult 실패: 삭제 카운트 0")
    void deleteResult_deletedZero() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(deepfakeDetectionRepository.findByDeepfakeDetectionIdAndUser(eq(100L), any(User.class)))
                .thenReturn(Optional.of(new DeepfakeDetection()));
        when(deepfakeDetectionRepository.deleteByDeepfakeDetectionIdAndUser(eq(100L), any(User.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.deleteResult(1L, 100L))
                .isInstanceOf(DetectionNotFoundException.class);
    }
}