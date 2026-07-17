package com.nurba.java.domain;

import com.nurba.java.enums.AuthProvider;
import com.nurba.java.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant createdAt;

    // Incremented on logout; refresh tokens with a different version are rejected.
    @Column(nullable = false)
    @Builder.Default
    private int tokenVersion = 0;

    // ── Telegram identity (nullable — populated on Telegram signup or later linking) ──
    private Long telegramId;
    private String telegramUsername;
    private String telegramFirstName;
    private String telegramLastName;
    private String telegramPhotoUrl;

    /** How the account was originally created. Not a login gate — see AuthProvider. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;
}
