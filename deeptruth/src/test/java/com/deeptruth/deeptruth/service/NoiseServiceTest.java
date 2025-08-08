package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.entity.Noise;
import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.repository.NoiseRepository;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoiseServiceTest {

    @InjectMocks
    private NoiseService noiseService;

    @Mock
    private NoiseRepository noiseRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void 사용자별_노이즈_기록_전체_조회_실패_사용자없음() {
        // given
        Long nonExistentUserId = 999L;

        when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> noiseService.getUserNoiseHistory(nonExistentUserId));
    }
}