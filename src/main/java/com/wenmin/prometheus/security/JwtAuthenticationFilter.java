package com.wenmin.prometheus.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            DecodedJWT jwt = jwtTokenProvider.verifyToken(token);
            if (jwt != null) {
                String userId = jwtTokenProvider.getUserId(jwt);
                String username = jwtTokenProvider.getUsername(jwt);
                List<String> roles = jwtTokenProvider.getRoles(jwt);

                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()
                        : List.of();

                SecurityUser user = new SecurityUser(userId, username, null, roles != null ? roles : List.of());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve token from Authorization header first, then fallback to httpOnly cookie.
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Try Authorization header (supports both raw token and "Bearer xxx" format)
        String headerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(headerToken)) {
            if (headerToken.startsWith("Bearer ")) {
                return headerToken.substring(7);
            }
            return headerToken;
        }

        // 2. Fallback to httpOnly cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
