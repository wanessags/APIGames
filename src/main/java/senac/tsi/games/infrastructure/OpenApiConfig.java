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
import org.springframework.context.annotation.Configuration;

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
                        - Idempotência em POST por X-Idempotency-Key.
                        - Rate limiting: 8 requisições por minuto; ao exceder, retorna 429 e bloqueia por 60 segundos.
                        - Versionamento demonstrado em GET /games/{id}/summary usando X-API-Version.

                        Fluxo recomendado para testar:
                        1. Clique em Authorize e use a chave games-demo-key, que possui permissão ADMIN.
                        2. Para POST, envie também X-Idempotency-Key com um valor único.
                        3. Consulte os links HATEOAS retornados para navegar entre recursos.
                        """,
                contact = @Contact(
                        name = "Wanessa Gonçalves",
                        email = "gwanessags@gmail.com"
                ),
                license = @License(name = "Uso acadêmico")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Ambiente local")
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
                        description = "CRUD e gerenciamento de chaves de API. Endpoints de chaves exigem X-API-Key com nível ADMIN e X-Idempotency-Key em operações POST.")
        },
        externalDocs = @ExternalDocumentation(
                description = "Swagger UI",
                url = "/swagger-ui/index.html"
        )
)
public class OpenApiConfig {
}
