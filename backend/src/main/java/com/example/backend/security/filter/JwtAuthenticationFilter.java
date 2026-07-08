package com.example.backend.security.filter;

import com.example.backend.exception.ErrorCode;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.model.enums.Status;
import com.example.backend.security.exception.FilterExceptionHandler;
import com.example.backend.service.UserService;
import com.example.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final FilterExceptionHandler filterExceptionHandler;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Jwt jwt = jwtUtil.verifyToken(token);

            String uuid = jwt.getSubject();

            if (uuid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userService.findByUuid(uuid);

                if (user.getStatus() != Status.ACTIVE) {
                    filterExceptionHandler.writeError(response, ErrorCode.ACCOUNT_INACTIVE.getStatus(), ErrorCode.ACCOUNT_INACTIVE.getMessage());
                    return;
                }

                List<GrantedAuthority> authorities = new ArrayList<>();
                for (Role role : user.getRoles()) {
                    authorities.add(new SimpleGrantedAuthority(role.getName()));
                }

                if ("access_token".equals(jwt.getClaim("type").toString())) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user.getUuid(), null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            filterExceptionHandler.writeError(response, ErrorCode.UNAUTHORIZED_ACCESS.getStatus(), ErrorCode.UNAUTHORIZED_ACCESS.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
