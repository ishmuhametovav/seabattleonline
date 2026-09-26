package com.example.seabattleonline.web.controller;

import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.service.AuthService;
import com.example.seabattleonline.service.UserService;
import com.example.seabattleonline.web.dto.auth.JwtRequest;
import com.example.seabattleonline.web.dto.auth.JwtResponse;
import com.example.seabattleonline.web.dto.user.UserDto;
import com.example.seabattleonline.web.dto.validation.OnCreate;
import com.example.seabattleonline.web.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Auth controller",
        description = "authorization API"
)
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    @Operation(
            summary = "login",
            description = "user login"
    )
    public JwtResponse login(
            @Validated @RequestBody @Parameter final JwtRequest loginRequest
    ) {
        return authService.login(loginRequest);
    }

    @PostMapping("/register")
    @Operation(
            summary = "registration",
            description = "user registration"
    )
    public UserDto register(
            @Validated(OnCreate.class) @RequestBody @Parameter
            final UserDto userDto
    ) {
        User user = userMapper.toEntity(userDto);
        User createdUser = userService.create(user);
        return userMapper.toDto(createdUser);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "refresh",
            description = "tokens refreshing"
    )
    public JwtResponse refresh(
            @RequestBody @Parameter final String refreshToken
    ) {
        return authService.refresh(refreshToken);
    }
}
