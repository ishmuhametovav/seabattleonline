package com.example.seabattleonline.web.mapper;

import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.web.dto.user.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends Mappable<UserDto, User> {
}
