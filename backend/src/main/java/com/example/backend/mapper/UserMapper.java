package com.example.backend.mapper;

import com.example.backend.dto.response.UserResponseDto;
import com.example.backend.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toResponse(User user);
}
