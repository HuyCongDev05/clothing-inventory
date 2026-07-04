package com.example.backend.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthResponseDto {
    @Getter
    @Setter
    public static class info {
        private String uuid;
        private String fullName;
        private String phone;
        private String email;
        private String createdAt;
        private String accessToken;
    }

    @Getter
    @Setter
    public static class Me {
        private String uuid;
        private String fullName;
        private String phone;
        private String email;
        private String createdAt;
    }

    @Getter
    @Setter
    public static class RefreshToken {
        private String accessToken;
    }
}
