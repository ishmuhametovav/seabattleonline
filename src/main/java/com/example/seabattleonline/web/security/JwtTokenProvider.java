package com.example.seabattleonline.web.security;

import com.example.seabattleonline.domain.exception.AccessDeniedException;
import com.example.seabattleonline.domain.user.Role;
import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.service.UserService;
import com.example.seabattleonline.service.prop.JwtProperties;
import com.example.seabattleonline.web.dto.auth.JwtResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret()
                .getBytes());
    }

    public String createAccessToken(
            final Long userId,
            final String username,
            final Role role
    ) {
        Instant validity = Instant.now()
                .plus(jwtProperties.getAccess(), ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(username)
                .claim("id", userId)
                .claim("role", role)
                .expiration(Date.from(validity))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(
            final Long userId,
            final String username,
            final Role role
    ) {
        Instant validity = Instant.now()
                .plus(jwtProperties.getRefresh(), ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(username)
                .claim("id", userId)
                .expiration(Date.from(validity))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(final String token) {
        Jws<Claims> claims = Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token);

        return !claims.getPayload().getExpiration()
                .before(new java.util.Date());
    }

    public JwtResponse refreshUserTokens(final String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new AccessDeniedException();
        }
        Long userId = Long.valueOf(getId(refreshToken));
        User user = userService.getById(userId);

        return new JwtResponse(userId, user.getName(),
                createAccessToken(userId, user.getName(), user.getRole()),
                createRefreshToken(userId, user.getName(), user.getRole()));
    }

    private String getName(final String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private String getId(final String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id")
                .toString();
    }

    public Authentication getAuthentication(final String token) {
        String username = getName(token);
        UserDetails userDetails = userDetailsService
                .loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities()
        );
    }
}
