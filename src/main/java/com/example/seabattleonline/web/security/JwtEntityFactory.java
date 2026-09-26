package com.example.seabattleonline.web.security;

import com.example.seabattleonline.domain.user.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class JwtEntityFactory {
    public static JwtEntity create(final User user) {
        return new JwtEntity(user.getId(), user.getName(), user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
