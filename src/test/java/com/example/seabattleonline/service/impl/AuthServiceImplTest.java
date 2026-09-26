package com.example.seabattleonline.service.impl;

import com.example.seabattleonline.domain.user.Role;
import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.service.UserService;
import com.example.seabattleonline.web.dto.auth.JwtRequest;
import com.example.seabattleonline.web.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {
    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login() {
        Long id = 1L;
        String username = "username";
        String password = "password";
        Role role = Role.ROLE_USER;
        User user = new User();
        user.setId(id);
        user.setName(username);
        user.setPassword(password);
        user.setRole(role);
        JwtRequest loginRequest = new JwtRequest(username, password);
        Mockito.when(userService.getByName(username)).thenReturn(user);
        authService.login(loginRequest);
        Mockito.verify(userService, Mockito.times(1)).getByName(username);
        Mockito.verify(authenticationManager, Mockito.times(1))
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.username(), loginRequest.password()
                        )
                );
        Mockito.verify(tokenProvider, Mockito.times(1))
                .createAccessToken(id, username, role);
        Mockito.verify(tokenProvider, Mockito.times(1))
                .createRefreshToken(id, username, role);
    }

    @Test
    void refresh() {
        String refreshToken = "refreshToken";
        authService.refresh(refreshToken);
        Mockito.verify(tokenProvider, Mockito.times(1))
                .refreshUserTokens(refreshToken);
    }
}
