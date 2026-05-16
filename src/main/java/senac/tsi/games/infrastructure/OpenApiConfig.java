package senac.tsi.games.infrastructure;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@SecurityScheme(
        name = "ApiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "Informe a chave de API no header X-API-Key. Para testes locais, use games-demo-key, que possui nível ADMIN."
)
@OpenAPIDefinition(
        info = @Info(
                title = "Games REST API",
                version = "2.0.0",
                description = """
                        API RESTful acadêmica para gerenciamento de um catálogo de jogos.

                        Recursos implementados:
                        - CRUD completo para jogos, categorias, plataformas, detalhes, usuários, reviews e chaves de API.
                        - Relacionamentos JPA: One-to-One, One-to-Many, Many-to-One e Many-to-Many.
                        - Paginação com Pageable nas listagens e buscas por coleção.
                        - HATEOAS com links self, update, delete e navegação entre recursos relacionados.
                        - Validação com Bean Validation e tratamento global de erros.
                        - Autenticação por X-API-Key para operações sensíveis, com níveis READ, WRITE e ADMIN.
                        - Autenticação por chave de API enviada no header X-API-Key.
                        - Gerenciamento de chaves com POST /api-keys usando uma chave ADMIN.
                        - Idempotência em POST por X-Idempotency-Key.
                        - Rate limiting: 8 requisições por minuto; ao exceder, retorna 429 e bloqueia por 60 segundos.
                        - Versionamento demonstrado em GET /games/{id}/summary usando X-API-Version.

                        Fluxo recomendado para testar:
                        1. Clique em Authorize.
                        2. Informe a chave games-demo-key.
                        3. O Swagger enviará automaticamente o header X-API-Key nos endpoints protegidos.
                        4. Para POST, envie também X-Idempotency-Key com um valor único.
                        5. Opcionalmente, gere novas chaves em POST /api-keys usando uma chave ADMIN.
                        6. Consulte os links HATEOAS retornados para navegar entre recursos.
                        """,
                contact = @Contact(
                        name = "Wanessa Gonçalves",
                        email = "gwanessags@gmail.com"
                ),
                license = @License(name = "Uso acadêmico")
        ),
        servers = {
                @Server(url = "https://apigames-kpkn.onrender.com", description = "Render")
        },
        tags = {
                @Tag(
                        name = "Games",
                        description = "Recurso central da API. Um Game pertence a uma Category (Many-to-One), possui um GameDetail (One-to-One), recebe várias Reviews (One-to-Many) e pode estar em várias Platforms (Many-to-Many)."),
                @Tag(
                        name = "Categories",
                        description = "Classificação dos jogos por enum CategoryType. Uma Category agrupa vários Games (One-to-Many)."),
                @Tag(
                        name = "Platforms",
                        description = "Plataformas de publicação, como PC, PlayStation e Xbox. Relação Many-to-Many com Games: um jogo pode estar em várias plataformas e uma plataforma pode ter vários jogos."),
                @Tag(
                        name = "GameDetails",
                        description = "Dados complementares de um jogo, como descrição e desenvolvedora. Relação One-to-One: cada Game possui, no máximo, um GameDetail."),
                @Tag(
                        name = "Reviews",
                        description = "Avaliações feitas por usuários para jogos. Cada Review aponta para um Game e para um User (Many-to-One nos dois lados)."),
                @Tag(
                        name = "Users",
                        description = "Usuários da API. Um User pode escrever várias Reviews e possuir várias ApiKeys (One-to-Many)."),
                @Tag(
                        name = "API Keys",
                        description = "CRUD e gerenciamento de chaves de API. Para testes acadêmicos, use games-demo-key no header X-API-Key. Novas chaves podem ser criadas em POST /api-keys usando uma chave ADMIN.")
        },
        externalDocs = @ExternalDocumentation(
                description = "Swagger UI",
                url = "/swagger-ui/index.html"
        )
)
public class OpenApiConfig {

    @Bean
    OpenApiCustomizer defaultResponsesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    addSuccessResponseIfMissing(method, operation);
                    addErrorResponseIfMissing(operation, "400", "Requisição inválida. Dados, parâmetros ou headers obrigatórios não foram enviados corretamente.");
                    addNotFoundIfNeeded(path, method, operation);
                    addConflictIfNeeded(method, operation);
                    addSecurityErrorsIfNeeded(operation);
                    addRateLimitIfMissing(operation);
                }));
    }

    private void addSuccessResponseIfMissing(PathItem.HttpMethod method, Operation operation) {
        if (method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.PUT) {
            addResponseIfMissing(operation, "200", "Operação realizada com sucesso.");
        }
        if (method == PathItem.HttpMethod.POST) {
            addResponseIfMissing(operation, "201", "Recurso criado com sucesso.");
        }
        if (method == PathItem.HttpMethod.DELETE) {
            addResponseIfMissing(operation, "204", "Recurso removido ou revogado com sucesso.");
        }
    }

    private void addNotFoundIfNeeded(String path, PathItem.HttpMethod method, Operation operation) {
        if (method == PathItem.HttpMethod.GET || path.contains("{")) {
            addErrorResponseIfMissing(operation, "404", "Recurso, busca ou página não encontrada para os parâmetros informados.");
        }
    }

    private void addConflictIfNeeded(PathItem.HttpMethod method, Operation operation) {
        if (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT || method == PathItem.HttpMethod.DELETE) {
            addErrorResponseIfMissing(operation, "409", "Conflito ao processar a operação. Pode ocorrer por chave idempotente reutilizada com payload diferente ou violação de integridade.");
        }
    }

    private void addSecurityErrorsIfNeeded(Operation operation) {
        boolean protectedEndpoint = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
        if (protectedEndpoint) {
            addErrorResponseIfMissing(operation, "401", "Não autorizado. Header X-API-Key ausente.");
            addErrorResponseIfMissing(operation, "403", "Acesso negado. X-API-Key inválida, inativa ou sem permissão para esta operação.");
        }
    }

    private void addRateLimitIfMissing(Operation operation) {
        if (!operation.getResponses().containsKey("429")) {
            operation.getResponses().addApiResponse("429", new ApiResponse()
                    .description("Muitas requisições. O limite de 8 requisições por minuto foi excedido; aguarde 60 segundos para tentar novamente.")
                    .addHeaderObject("Retry-After", new Header()
                            .description("Quantidade de segundos que o cliente deve aguardar antes de tentar novamente.")
                            .schema(new IntegerSchema().example(60)))
                    .addHeaderObject("X-RateLimit-Limit", new Header()
                            .description("Limite máximo de requisições por minuto.")
                            .schema(new IntegerSchema().example(8)))
                    .addHeaderObject("X-RateLimit-Remaining", new Header()
                            .description("Quantidade de requisições restantes na janela atual.")
                            .schema(new IntegerSchema().example(0)))
                    .content(new Content().addMediaType("application/json", new MediaType()
                            .schema(new Schema<Map<String, Object>>()
                                    .type("object")
                                    .example(Map.of(
                                            "status", 429,
                                            "error", "Too Many Requests",
                                            "message", "Limite de 8 requisições por minuto excedido. Tente novamente em 60 segundos.",
                                            "retryAfter", 60))))));
        }
    }

    private void addErrorResponseIfMissing(Operation operation, String code, String description) {
        if (!operation.getResponses().containsKey(code)) {
            int status = Integer.parseInt(code);
            operation.getResponses().addApiResponse(code, new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType("application/json", new MediaType()
                            .schema(new Schema<ApiError>()
                                    .example(Map.of(
                                            "timestamp", "2026-05-15T12:00:00Z",
                                            "status", status,
                                            "error", description,
                                            "message", description,
                                            "path", "/games/999",
                                            "fields", Map.of()))))));
        }
    }

    private void addResponseIfMissing(Operation operation, String code, String description) {
        if (!operation.getResponses().containsKey(code)) {
            operation.getResponses().addApiResponse(code, new ApiResponse().description(description));
        }
    }
}
