package com.example.backend.controller;

import com.example.backend.dto.request.UserUpdateRequestDto;
import com.example.backend.dto.response.UserResponseDto;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable String uuid,
            @Valid @RequestBody UserUpdateRequestDto request) {
        return ResponseEntity.ok(userService.updateUser(uuid, request));
    }
}
