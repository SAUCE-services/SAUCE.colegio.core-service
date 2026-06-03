package ar.com.sauce.colegio.rest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.api.key:default-secret-key}")
    private String apiKey;

    private static final String API_KEY_HEADER = "X-API-KEY";

    // 🌟 FORMA NATIVA DE SPRING: Si da true, el filtro directamente se apaga para estas URLs
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-resources");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Permitimos peticiones OPTIONS para CORS de forma directa
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        // Validación estricta de la llave para el resto de los módulos de la escuela
        if (apiKey.equals(requestApiKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
        }
    }
}