package com.example.sih26060.security;

import com.example.sih26060.entity.Role;
import com.example.sih26060.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Carries role + station scope through Spring Security so controllers can read it via
 * {@code @AuthenticationPrincipal} without a second lookup. stationId is null for
 * HQ_ADMIN — that absence of a station is exactly what grants unrestricted access in
 * AuthorizationSupport.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Long stationId;
    private final String stationName;

    public UserPrincipal(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.stationId = user.getStationId();
        this.stationName = user.getStation() != null ? user.getStation().getName() : null;
    }

    public boolean isHqAdmin() {
        return role == Role.HQ_ADMIN;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
