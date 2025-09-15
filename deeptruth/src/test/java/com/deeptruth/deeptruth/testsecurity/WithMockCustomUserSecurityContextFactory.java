package com.deeptruth.deeptruth.testsecurity;

import com.deeptruth.deeptruth.config.CustomUserDetails;
import com.deeptruth.deeptruth.entity.User;
import com.deeptruth.deeptruth.base.Enum.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        User user = User.builder()
                .userId(annotation.userId())
                .loginId(annotation.loginId())
                .email(annotation.email())
                .nickname(annotation.nickname())
                .name(annotation.name())
                .password("N/A")
                .role(Role.valueOf(annotation.role()))
                .build();

        CustomUserDetails principal = new CustomUserDetails(user);

        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
        );
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        return ctx;
    }
}