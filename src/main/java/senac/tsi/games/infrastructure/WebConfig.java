package senac.tsi.games.infrastructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Web que registra o RateLimitInterceptor
 * para todas as rotas da API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;

    @Autowired
    public WebConfig(RateLimitInterceptor rateLimitInterceptor,
                     ApiKeyInterceptor apiKeyInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://apigames-kpkn.onrender.com",
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://127.0.0.1:5500")
                .allowedMethods(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name())
                .allowedHeaders("Content-Type", "X-API-Key", "X-Idempotency-Key", "X-API-Version")
                .exposedHeaders("Location", "Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset")
                .maxAge(3600);
    }
}
