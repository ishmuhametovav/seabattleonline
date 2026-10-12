package com.example.seabattleonline.web.dto.user;

import com.example.seabattleonline.web.dto.validation.OnCreate;
import com.example.seabattleonline.web.dto.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "User dto")
public record UserDto(
        @Schema(description = "user id", example = "1")
        @NotNull(message = "must be not null", groups = {OnUpdate.class})
        Long id,

        @Schema(description = "name", example = "habibu")
        @NotBlank(message = "name must be not null")
        @Size(max = 255, message = "name must be less than 256")
        String name,

        @Schema(description = "email", example = "habibu@mail.com")
        @Size(max = 255, message = "email must be less than 256")
        String email,

        @Schema(description = "password", example = "123")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "password must be not null")
        @Size(max = 255, message = "password must be less than 256")
        String password,

        @Schema(description = "password confirmation", example = "123")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "passwordConfirmation must be not null",
                groups = {OnCreate.class}
        )
        @Size(max = 255, message = "passwordConfirmation must be less than 256",
                groups = {OnCreate.class}
        )
        String passwordConfirmation
) {
}
