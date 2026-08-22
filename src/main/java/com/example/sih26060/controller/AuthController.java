package com.example.sih26060.controller;

import com.example.sih26060.dto.LoginRequest;
import com.example.sih26060.dto.LoginResponse;
import com.example.sih26060.dto.UserInfo;
import com.example.sih26060.security.JwtService;
import com.example.sih26060.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String token = jwtService.generateToken(principal);
            return ResponseEntity.ok(new LoginResponse(token, toUserInfo(principal)));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }
    }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal UserPrincipal principal) {
        return toUserInfo(principal);
    }

    private UserInfo toUserInfo(UserPrincipal principal) {
        return new UserInfo(principal.getUsername(), principal.getRole(),
                principal.getStationId(), principal.getStationName());
    }
}
