package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.dto.watermark.WatermarkDTO;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.entity.Watermark;
import com.deeptruth.deeptruth.repository.UserRepository;
import com.deeptruth.deeptruth.repository.WatermarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WatermarkServiceTest {
    @Mock
    private WatermarkRepository watermarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AmazonS3Service amazonS3Service;

    @InjectMocks
    private WatermarkService watermarkService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllResult_ShouldReturnDTOList() {
        // given
        Long userId = 1L;
        User user = new User();
        List<Watermark> marks = List.of(
                Watermark.builder().user(user).originalFilePath("a.jpg").watermarkedFilePath("b.jpg").build()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(watermarkRepository.findAllByUser(user)).thenReturn(marks);

        // when
        List<WatermarkDTO> result = watermarkService.getAllResult(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalFilePath()).isEqualTo("a.jpg");
    }

    @Test
    void getSingleResult_ShouldReturnDTO() {
        // given
        Long userId = 1L;
        Long markId = 2L;
        User user = new User();
        Watermark mark = Watermark.builder().originalFilePath("a.jpg").watermarkedFilePath("b.jpg").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(watermarkRepository.findByWatermarkIdAndUser(markId, user)).thenReturn(Optional.of(mark));

        // when
        WatermarkDTO result = watermarkService.getSingleResult(userId, markId);

        // then
        assertThat(result.getOriginalFilePath()).isEqualTo("a.jpg");
    }

    @Test
    void deleteWatermark_ShouldInvokeRepositoryDelete() {
        // given
        Long userId = 1L;
        Long markId = 2L;
        User user = new User();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        watermarkService.deleteWatermark(userId, markId);

        // then
        verify(watermarkRepository).deleteByWatermarkIdAndUser(markId, user);
    }

}
