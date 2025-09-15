package com.deeptruth.deeptruth.testsecurity;


import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    long userId() default 1L;
    String loginId() default "testLogin";
    String email() default "test@example.com";
    String nickname() default "tester";
    String name() default "테스트유저";
    String role() default "USER";
}