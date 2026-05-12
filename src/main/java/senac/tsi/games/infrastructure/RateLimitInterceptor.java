package senac.tsi.games.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 8;
    private static final long WINDOW_MS = 60_000;
    private static final long BLOCK_MS = 60_000;

    private final Map<String, RateLimitState> requestCounts = new ConcurrentHashMap<>();

    private record RateLimitState(long count, long windowStart, long blockedUntil) {
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (shouldIgnore(request)) {
            return true;
        }

        String clientKey = getClientKey(request);
        long now = Instant.now().toEpochMilli();

        RateLimitState state = requestCounts.compute(clientKey, (key, current) -> {
            if (current != null && current.blockedUntil() > now) {
                return current;
            }

            if (current == null || current.blockedUntil() > 0 || now - current.windowStart() >= WINDOW_MS) {
                return new RateLimitState(1, now, 0);
            }

            long nextCount = current.count() + 1;
            if (nextCount > MAX_REQUESTS) {
                return new RateLimitState(nextCount, current.windowStart(), now + BLOCK_MS);
            }
            return new RateLimitState(nextCount, current.windowStart(), 0);
        });

        boolean blocked = state.blockedUntil() > now;
        long resetAt = blocked ? state.blockedUntil() : state.windowStart() + WINDOW_MS;
        long retryAfterSeconds = Math.max(1, (long) Math.ceil((resetAt - now) / 1000.0));
        long remaining = blocked ? 0 : Math.max(0, MAX_REQUESTS - state.count());

        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetAt / 1000));

        if (blocked) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Limite de %d requisicoes por minuto excedido. Tente novamente em %d segundos.",
                      "retryAfter": %d
                    }
                    """, MAX_REQUESTS, retryAfterSeconds, retryAfterSeconds));
            return false;
        }

        return true;
    }

    private boolean shouldIgnore(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (path.equals("/")
                || path.equals("/favicon.ico")
                || path.equals("/error")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console")) {
            return true;
        }

        return !(path.startsWith("/games")
                || path.startsWith("/categories")
                || path.startsWith("/platforms")
                || path.startsWith("/reviews")
                || path.startsWith("/users")
                || path.startsWith("/game-details")
                || path.startsWith("/api-keys"));
    }

    private String getClientKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "api-key:" + apiKey;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
