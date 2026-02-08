package jwt;

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
import org.springframework.web.filter.OncePerRequestFilter;
import repository.AdminRepository;
import model.Admin;
import service.SessionActivityService;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final SessionActivityService sessionActivityService;

    public JwtAuthFilter(JwtService jwtService, AdminRepository adminRepository,
                         SessionActivityService sessionActivityService) {
        this.jwtService = jwtService;
        this.adminRepository = adminRepository;
        this.sessionActivityService = sessionActivityService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/admin/login") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = JwtService.extractTokenFromRequest(request);

        if (token != null && jwtService.validateToken(token)) {
            try {
                Claims claims = jwtService.getClaims(token);
                String username = claims.getSubject();

                var adminOpt = adminRepository.findByUsername(username);
                if (adminOpt.isEmpty() || !adminOpt.get().isActive()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                Admin admin = adminOpt.get();
                Long adminId = admin.getId();

                if (!sessionActivityService.isSessionValid(adminId)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Session expired due to inactivity. Please log in again.\"}");
                    return;
                }
                sessionActivityService.updateActivity(adminId);

                var now = java.time.LocalDateTime.now();
                var last = admin.getLastLoginAt();
                if (last == null || ChronoUnit.MINUTES.between(last, now) >= 1) {
                    admin.setLastLoginAt(now);
                    adminRepository.save(admin);
                }

                String roleFromToken = claims.get("role", String.class);

                if (roleFromToken != null) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleFromToken);
                    List<SimpleGrantedAuthority> authorities = List.of(authority);

                    System.out.println("DEBUG: JWT Validiran. Korisnik: " + username + " | Autoritet: " + authorities);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                System.err.println("DEBUG: Greška pri obradi tokena: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}