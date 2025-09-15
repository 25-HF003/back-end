package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermark.InsertResultDTO;
import com.deeptruth.deeptruth.base.dto.watermark.WatermarkFlaskResponseDTO;
import com.deeptruth.deeptruth.base.exception.ExternalServiceException;
import com.deeptruth.deeptruth.base.exception.ImageDecodingException;
import com.deeptruth.deeptruth.base.exception.UserNotFoundException;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WatermarkServiceTest {
    @Mock private WatermarkRepository watermarkRepository;
    @Mock private UserRepository userRepository;
    @Mock private AmazonS3Service amazonS3Service;
    @Mock private WebClient webClient;

    // WebClient fluent 체인 목들
    @Mock private WebClient.RequestBodyUriSpec uriSpec;
    @Mock private WebClient.RequestBodySpec bodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private WatermarkService watermarkService;

    @BeforeEach
    void setUp() {
        try {
            var f = WatermarkService.class.getDeclaredField("flaskServerUrl");
            f.setAccessible(true);
            f.set(watermarkService, "http://fake-flask.local");
        } catch (Exception ignored) {}
    }

    private void mockWebClientReturning(WatermarkFlaskResponseDTO dto) {
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(eq(MediaType.MULTIPART_FORM_DATA))).thenReturn(bodySpec);
        when(bodySpec.body(any(BodyInserters.MultipartInserter.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(WatermarkFlaskResponseDTO.class))).thenReturn(Mono.justOrEmpty(dto));
    }


    @Test
    @DisplayName("insert 성공: Flask 응답을 받아 S3 업로드 후 DB 저장 및 DTO 반환")
    void insert_success() throws Exception {
        // given
        long userId = 10L;
        var user = User.builder()
                .userId(userId)
                .email("u@test.com")
                .loginId("login")
                .name("name")
                .nickname("nick")
                .password("pwd")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Flask가 base64 이미지 반환
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0x00FF00); // 임의의 색 한 픽셀
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] tinyPng = baos.toByteArray();

        var flaskDto = new WatermarkFlaskResponseDTO();
        flaskDto.setImage_base64(java.util.Base64.getEncoder().encodeToString(tinyPng));
        flaskDto.setFilename("watermarked.png");
        mockWebClientReturning(flaskDto);

        when(amazonS3Service.uploadStream(
                any(InputStream.class),
                argThat(k -> k != null && k.endsWith("watermarked.png")),
                eq("image/png")
        )).thenReturn("https://s3.example/watermarks/watermarked.png");

        when(amazonS3Service.uploadStream(
                any(InputStream.class),
                argThat(k -> k != null && k.endsWith("message.txt")),
                eq("text/plain")
        )).thenReturn("https://s3.example/watermarks/message.txt");

        // DB save mock
        when(watermarkRepository.save(any(Watermark.class)))
                .thenAnswer(inv -> inv.getArgument(0, Watermark.class));

        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "input.png", "image/png", tinyPng
        );

        // when
        InsertResultDTO result = watermarkService.insert(userId, file, "abcd", "task-1");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("watermarked.png");
        assertThat(result.getS3WatermarkedKey()).isEqualTo("https://s3.example/watermarks/watermarked.png");
        assertThat(result.getMessage()).isEqualTo("abcd");
        assertThat(result.getTaskId()).isEqualTo("task-1");

        verify(userRepository).findById(userId);
        verify(webClient).post();
        verify(watermarkRepository).save(any(Watermark.class));
        verify(amazonS3Service, times(2)).uploadStream(any(InputStream.class), anyString(), anyString());
    }

    @Test
    @DisplayName("insert 실패: 유저 없음 -> UserNotFoundException")
    void insert_userNotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "f.png", "image/png", new byte[]{1,2});

        // when & then
        assertThatThrownBy(() -> watermarkService.insert(999L, file, "ab", "tid"))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(webClient);
        verifyNoInteractions(amazonS3Service);
        verify(watermarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("insert 실패: 메세지 5자 이상 -> IllegalArgumentException")
    void insert_messageTooLong() {
        var file = new MockMultipartFile("file", "f.png", "image/png", new byte[]{1,2});
        assertThatThrownBy(() -> watermarkService.insert(1L, file, "12345", "tid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 4자");
    }


    @Test
    void insert_flaskNull() throws Exception {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.builder().userId(userId).build()));

        // 유효한 PNG 바이트 생성
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] validPng = baos.toByteArray();
        mockWebClientReturning(null);

        MockMultipartFile file = new MockMultipartFile(
                "file", "input.png", "image/png", validPng);

        // 실행 & 검증
        assertThatThrownBy(() -> watermarkService.insert(userId, file, "abcd", "task-1"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("비어"); // "Flask 서버 응답이 비어 있습니다."
    }

    @Test
    @DisplayName("insert 실패: Flask base64 디코딩 실패 -> ImageDecodingException")
    void insert_invalidBase64() throws Exception {
        // given
        long userId = 1L;
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.builder().userId(userId).email("a@a.com").loginId("a").name("n").nickname("n").password("p").build()));

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] validPng = baos.toByteArray();

        MockMultipartFile file =
                new MockMultipartFile("file", "input.png", "image/png", validPng);

        // Flask가 "깨진 base64" 반환하도록 목킹
        WatermarkFlaskResponseDTO flaskDto = new WatermarkFlaskResponseDTO();
        flaskDto.setFilename("watermarked.png");
        flaskDto.setImage_base64("!!not-base64!!"); // 디코딩 실패 유도
        mockWebClientReturning(flaskDto);
        assertThatThrownBy(() -> watermarkService.insert(userId, file, "ab", "tid"))
                .isInstanceOf(ImageDecodingException.class);
    }

    @Test
    @DisplayName("getAllResult 성공: 페이지 결과 반환")
    void getAllResult_success() {
        long userId = 7L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().userId(userId).build()));

        var entity = Watermark.builder()
                .watermarkId(1L)
                .artifactId("art")
                .fileName("f.png")
                .message("ab")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Watermark> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(watermarkRepository.findByUser_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(page);

        var result = watermarkService.getAllResult(userId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getArtifactId()).isEqualTo("art");
    }

    @Test
    @DisplayName("getSingleResult 실패: 해당 사용자에 결과 없음 -> WatermarkNotFoundException")
    void getSingleResult_notFound() {
        long userId = 5L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().userId(userId).build()));
        when(watermarkRepository.findByWatermarkIdAndUser(eq(999L), any(User.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> watermarkService.getSingleResult(userId, 999L))
                .hasMessageContaining("not found");
    }


    @Test
    @DisplayName("deleteWatermark 실패: 삭제수 0 -> WatermarkNotFoundException")
    void deleteWatermark_notFound() {
        long userId = 3L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().userId(userId).build()));
        when(watermarkRepository.deleteByWatermarkIdAndUser(eq(100L), any(User.class))).thenReturn(0);

        assertThatThrownBy(() -> watermarkService.deleteWatermark(userId, 100L))
                .hasMessageContaining("not found");
    }

}
