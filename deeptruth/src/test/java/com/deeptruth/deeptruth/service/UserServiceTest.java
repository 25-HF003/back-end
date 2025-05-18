package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 소셜로그인_기존_유저는_조회만_한다() {
        // given
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.getEmail()).thenReturn("hong@example.com");
        when(userRepository.findByEmail("hong@example.com"))
                .thenReturn(Optional.of(User.builder().email("hong@example.com").build()));

        // when
        User result = userService.findOrCreateSocialUser(userInfo, "google");

        // then
        assertThat(result.getEmail()).isEqualTo("hong@example.com");
        verify(userRepository, never()).save(any());

    }


    @Test
    void 소셜로그인_신규_유저는_DB에_저장된다() {
        // given
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.getEmail()).thenReturn("newuser@example.com");
        when(userInfo.getName()).thenReturn("신규유저");

        when(userRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.empty());

        // save()가 저장한 User를 그대로 반환해야 result가 null이 아님
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.findOrCreateSocialUser(userInfo, "google");

        // then
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        verify(userRepository).save(any(User.class));

    }

}
