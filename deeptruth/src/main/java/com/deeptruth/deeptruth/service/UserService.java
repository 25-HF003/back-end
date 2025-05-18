package com.deeptruth.deeptruth.service;

import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import com.deeptruth.deeptruth.base.OAuth.OAuth2UserInfo;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findOrCreateSocialUser(OAuth2UserInfo oAuth2UserInfo, String provider){
//        String email = oAuth2UserInfo.getEmail();
//        return userRepository.findByEmail(email)
//                .orElseGet(()-> userRepository.save(User.builder()
//                        .name(oAuth2UserInfo.getName())
//                        .email(email)
//                        .loginId(provider+"_"+ UUID.randomUUID())
//                        .nickname(oAuth2UserInfo.getName())
//                        .password("NO_PASSWORD")
//                        .role(Role.USER)
//                        .socialLoginType(SocialLoginType.valueOf(provider.toUpperCase()))
//                        .build()));
        return userRepository.findByEmail(oAuth2UserInfo.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(oAuth2UserInfo.getEmail())
                            .name(oAuth2UserInfo.getName())
                            .loginId(provider + "_" + UUID.randomUUID())
                            .nickname(oAuth2UserInfo.getName())
                            .password("NO_PASSWORD")
                            .role(Role.USER)
                            .socialLoginType(SocialLoginType.valueOf(provider.toUpperCase()))
                            .build();

                    return userRepository.save(newUser); // ✅ 이 줄이 있어야 테스트 통과
                });
    }


}
