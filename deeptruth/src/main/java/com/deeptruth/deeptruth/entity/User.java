package com.deeptruth.deeptruth.entity;

import com.deeptruth.deeptruth.base.Enum.Level;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable=false, length = 50)
    private String name;

    @Column(nullable=false, unique = true, length = 50)
    private String loginId;

    @Column(nullable=false, length = 255)
    private String password;

    @Column(nullable=false, unique = true, length = 50)
    private String nickname;

    @Column(nullable=false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Level level = Level.EXPLORER;

    @Column(nullable=false)
    @Builder.Default
    private int point = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SocialLoginType socialLoginType = SocialLoginType.NONE;

    @Column(nullable=false, updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }
}
