package com.example.seabattleonline.service.impl;

import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.service.AuthService;
import com.example.seabattleonline.service.UserService;
import com.example.seabattleonline.web.dto.auth.JwtRequest;
import com.example.seabattleonline.web.dto.auth.JwtResponse;
import com.example.seabattleonline.web.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Override
    public JwtResponse login(final JwtRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(), loginRequest.password()
                )
        );
        User user = userService.getByName(loginRequest.username());
        return new JwtResponse(user.getId(), user.getName(),
                tokenProvider.createAccessToken(
                        user.getId(), user.getName(), user.getRole()
                ),
                tokenProvider.createRefreshToken(
                        user.getId(), user.getName(), user.getRole()
                )
        );
    }

    @Override
    public JwtResponse refresh(final String refreshToken) {
        return tokenProvider.refreshUserTokens(refreshToken);
    }

}
