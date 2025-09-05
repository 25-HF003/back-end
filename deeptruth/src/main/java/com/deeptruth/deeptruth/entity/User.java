package com.deeptruth.deeptruth.entity;

import com.deeptruth.deeptruth.base.Enum.Level;
import com.deeptruth.deeptruth.base.Enum.Role;
import com.deeptruth.deeptruth.base.Enum.SocialLoginType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
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

    @Column(columnDefinition = "TEXT")
    private String signature;

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

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable=false, updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
