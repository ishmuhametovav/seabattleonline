package com.example.seabattleonline.service;

import com.example.seabattleonline.web.dto.auth.JwtRequest;
import com.example.seabattleonline.web.dto.auth.JwtResponse;

public interface AuthService {
    JwtResponse login(JwtRequest loginRequest);

    JwtResponse refresh(String refreshToken);
}
