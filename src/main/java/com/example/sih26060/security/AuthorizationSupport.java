package com.example.sih26060.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Station-scoping rule shared by every list endpoint: HQ_ADMIN (stationId == null on the
 * principal) sees whatever it asks for, including everything when no stationId is given.
 * STATION_MANAGER/CREW are pinned to their own station — an unscoped request is narrowed
 * to it automatically, and a request for a *different* station is rejected outright.
 */
@Component
public class AuthorizationSupport {

    public Long resolveStationId(UserPrincipal principal, Long requestedStationId) {
        if (principal.isHqAdmin()) {
            return requestedStationId;
        }
        if (requestedStationId != null && !requestedStationId.equals(principal.getStationId())) {
            throw new AccessDeniedException("Not authorized for station " + requestedStationId);
        }
        return principal.getStationId();
    }
}
