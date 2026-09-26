package com.example.seabattleonline.web.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
@AllArgsConstructor
public class JwtEntity implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private final Collection<? extends GrantedAuthority> authorities;
}
