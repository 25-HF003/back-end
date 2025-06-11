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

        return userRepository.findByEmail(oAuth2UserInfo.getEmail())
                .orElseGet(() ->
                    userRepository.save(User.builder()
                            .email(oAuth2UserInfo.getEmail())
                            .name(oAuth2UserInfo.getName())
                            .loginId(provider + "_" + UUID.randomUUID())
                            .nickname(generateUniqueNickname(oAuth2UserInfo.getName()))
                            .password("NO_PASSWORD")
                            .role(Role.USER)
                            .socialLoginType(SocialLoginType.valueOf(provider.toUpperCase()))
                            .build()
                ));
    }


    private String generateUniqueNickname(String baseName){
        String nickname = baseName;
        int count=1;
        while(userRepository.existsByNickname(nickname)){
            nickname = baseName + "_" + count;
            count++;
        }
        return nickname;
    }

    public boolean existsByUserId(Long userId){
        return userRepository.existsByUserId(userId);
    }
}
