package com.deeptruth.deeptruth.base.dto.user;

import com.deeptruth.deeptruth.base.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private Long userId;
    private String loginId;
    private String name;
    private String nickname;
    private String email;
    private Role role;
}
