package com.example.sih26060.dto;

public record LoginResponse(
        String token,
        UserInfo user
) {
}
