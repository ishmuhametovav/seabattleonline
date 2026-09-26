package com.example.seabattleonline.web.security.expression;

import com.example.seabattleonline.domain.user.Role;
import com.example.seabattleonline.service.UserService;
import com.example.seabattleonline.web.security.JwtEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("customSecurityExpression")
@RequiredArgsConstructor
public class CustomSecurityExpression {
    private final UserService userService;

    public boolean canAccessUser(final Long id) {
        JwtEntity authorizedUser = (JwtEntity) SecurityContextHolder
                .getContext()
                .getAuthentication().getPrincipal();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                Role.ROLE_ADMIN.name()
        );

        return authorizedUser.getId().equals(id)
                || authorizedUser.getAuthorities().contains(authority);
    }
}
