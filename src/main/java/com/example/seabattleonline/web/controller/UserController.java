package com.example.seabattleonline.web.controller;

import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.service.UserService;
import com.example.seabattleonline.web.dto.user.UserDto;
import com.example.seabattleonline.web.dto.validation.OnUpdate;
import com.example.seabattleonline.web.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(
        name = "User controller",
        description = "user controller API"
)
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/health")
    @Operation(summary = "check server status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "server is OK"),
            @ApiResponse(responseCode = "500", description = "server problems")
    })
    public boolean healthCheck() {
        return true;
    }

    @GetMapping("/{id}")
    @Operation(summary = "get user by id")
    @ApiResponses(value = {})
    @PreAuthorize("@customSecurityExpression.canAccessUser(#id)")
    public UserDto getById(@PathVariable @Parameter final Long id) {
        User user = userService.getById(id);
        return userMapper.toDto(user);
    }

    @PutMapping
    @Operation(summary = "update user")
    @ApiResponses(value = {})
    @PreAuthorize("@customSecurityExpression.canAccessUser(#userDto.id)")
    public UserDto update(
            @Validated(OnUpdate.class) @RequestBody @Parameter
            final UserDto userDto
    ) {
        User user = userMapper.toEntity(userDto);
        User updatedUser = userService.update(user);
        return userMapper.toDto(updatedUser);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete user by id")
    @ApiResponses(value = {})
    @PreAuthorize("@customSecurityExpression.canAccessUser(#id)")
    public void deleteById(@PathVariable @Parameter final Long id) {
        userService.deleteById(id);
    }
}
