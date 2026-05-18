package senac.tsi.games.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import senac.tsi.games.entities.ApiKeyRole;
import senac.tsi.games.repositories.ApiKeyRepository;

import java.io.IOException;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyInterceptor(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!requiresApiKey(request)) {
            return true;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            writeError(request, response, HttpStatus.UNAUTHORIZED, "Header X-API-Key é obrigatório para esta operação.");
            return false;
        }

        return apiKeyRepository.findByKeyValueAndActiveTrue(apiKey)
                .map(validKey -> {
                    ApiKeyRole requiredRole = requiredRole(request);
                    if (!hasAccess(validKey.getRole(), requiredRole)) {
                        try {
                            writeError(request, response, HttpStatus.FORBIDDEN, "X-API-Key sem permissão para esta operação. Nível exigido: " + requiredRole + ".");
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                        return false;
                    }
                    return true;
                })
                .orElseGet(() -> {
                    try {
                        writeError(request, response, HttpStatus.FORBIDDEN, "X-API-Key inválida ou inativa.");
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                    return false;
                });
    }

    private boolean requiresApiKey(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.OPTIONS.matches(method)
                || path.equals("/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console")) {
            return false;
        }

        if (path.contains("/api-keys")) {
            return true;
        }

        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method);
    }

    private ApiKeyRole requiredRole(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.contains("/api-keys")) {
            return ApiKeyRole.ADMIN;
        }

        if (HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method)) {
            return ApiKeyRole.WRITE;
        }

        return ApiKeyRole.READ;
    }

    private boolean hasAccess(ApiKeyRole currentRole, ApiKeyRole requiredRole) {
        if (currentRole == ApiKeyRole.ADMIN) {
            return true;
        }
        if (currentRole == ApiKeyRole.WRITE) {
            return requiredRole == ApiKeyRole.WRITE || requiredRole == ApiKeyRole.READ;
        }
        return currentRole == ApiKeyRole.READ && requiredRole == ApiKeyRole.READ;
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) throws IOException {
        addCorsHeaders(request, response);
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(status.value(), status.getReasonPhrase(), message));
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", allowedOrigin(origin));
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-API-Key,X-Idempotency-Key,X-API-Version,Accept");
        response.setHeader("Access-Control-Expose-Headers", "Location,Retry-After,X-RateLimit-Limit,X-RateLimit-Remaining,X-RateLimit-Reset");
    }

    private String allowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return "https://apigames-kpkn.onrender.com";
        }
        if (origin.equals("https://apigames-kpkn.onrender.com")
                || origin.equals("http://localhost:8080")
                || origin.equals("http://localhost:3000")
                || origin.equals("http://localhost:5173")
                || origin.equals("http://127.0.0.1:5500")) {
            return origin;
        }
        return "https://apigames-kpkn.onrender.com";
    }
}
