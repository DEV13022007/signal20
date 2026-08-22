package com.example.sih26060.dto;

import com.example.sih26060.entity.Role;

public record UserInfo(
        String username,
        Role role,
        Long stationId,
        String stationName
) {
}
