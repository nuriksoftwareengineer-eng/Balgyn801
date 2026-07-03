package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Server-side record of an issued refresh token, keyed by its JWT id ({@code jti}).
 *
 * <p>Enables refresh-token <b>rotation</b> (each refresh revokes the used token and issues a new one)
 * and <b>reuse detection</b> (replaying an already-rotated/revoked token revokes the whole session
 * family). Multi-device safe: each login/device gets its own jti chain.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
