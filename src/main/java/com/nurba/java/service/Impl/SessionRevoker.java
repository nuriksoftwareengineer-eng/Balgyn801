package com.nurba.java.service.Impl;

import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes all of a user's refresh sessions in an INDEPENDENT transaction.
 *
 * <p>Used by refresh reuse-detection: that path must persist the revoke-all AND then throw to
 * reject the request. Doing both in the caller's transaction would roll the revoke back on the
 * throw, so this runs in {@code REQUIRES_NEW} and commits before the caller's transaction unwinds.
 * It is a separate bean so the Spring proxy actually applies the new-transaction semantics
 * (self-invocation would bypass it).
 */
@Component
@RequiredArgsConstructor
public class SessionRevoker {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllSessions(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        appUserRepository.findById(userId).ifPresent(u -> {
            u.setTokenVersion(u.getTokenVersion() + 1);
            appUserRepository.save(u);
        });
    }
}
