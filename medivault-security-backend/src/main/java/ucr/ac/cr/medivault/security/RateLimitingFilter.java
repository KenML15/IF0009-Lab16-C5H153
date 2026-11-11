package ucr.ac.cr.medivault.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int LOGIN_LIMIT = 5;   
    private static final int API_LIMIT = 30;    

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = extractClientIp(request);
        Bucket target = null;

        if (path.startsWith("/api/v1/auth/login")) {
            target = loginBuckets.computeIfAbsent(ip, k -> newBucket(LOGIN_LIMIT));
        } else if (path.startsWith("/api/v1/expedientes")) {
            target = apiBuckets.computeIfAbsent(ip, k -> newBucket(API_LIMIT));
        }

        if (target == null || target.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write("""
            {"type":"https://medivault.ucr.ac.cr/rate-limit",
             "title":"Demasiadas Peticiones","status":429,
             "detail":"Limite superado. Espere 60s.",
             "instance":"%s"}""".formatted(path));
    }

    private Bucket newBucket(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity,
                        Refill.intervally(capacity, Duration.ofMinutes(1))))
                .build();
    }

   public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}