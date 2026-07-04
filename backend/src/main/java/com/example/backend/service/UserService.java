package com.example.backend.service;

import com.example.backend.dto.request.UserUpdateRequestDto;
import com.example.backend.dto.response.UserResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto updateUser(String uuid, UserUpdateRequestDto request) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new InvalidException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new InvalidException(ErrorCode.CONFLICT_USER_EMAIL);
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new InvalidException(ErrorCode.CONFLICT_USER_PHONE);
            }
            user.setPhone(request.getPhone());
        }

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        return userMapper.toResponse(user);
    }
}
