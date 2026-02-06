package com.ws.backend.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Webhook od PSP-a ne zahteva JWT - preskačemo filter
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/payment/callback");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = JwtService.extractTokenFromRequest(request);
        System.out.println("JwtAuthFilter - Request URI: " + request.getRequestURI());
        System.out.println("JwtAuthFilter - Token extracted: " + (token != null ? "Yes (length: " + token.length() + ")" : "No"));
        
        if (token == null) {
            System.out.println("JwtAuthFilter - No token found, continuing without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        boolean isValid = jwtService.validateToken(token);
        System.out.println("JwtAuthFilter - Token valid: " + isValid);
        
        if (isValid) {
            try {
                Claims claims = jwtService.getClaims(token);
                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);

                String jti = claims.getId();

                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                System.out.println("JWT claims -> email: " + email + ", role: " + role + ", userId: " + userId + ", Jti: " + jti);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("JwtAuthFilter - Authentication set in SecurityContext");
            } catch (Exception e) {
                System.err.println("JwtAuthFilter - Error processing token: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("JwtAuthFilter - Token validation failed for request: " + request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
