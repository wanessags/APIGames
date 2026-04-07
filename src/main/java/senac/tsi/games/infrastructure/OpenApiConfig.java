/*
 * Este arquivo pertence à infraestrutura da aplicação.
 * Essa camada concentra configurações ou inicializações que dão suporte ao projeto, como Swagger/OpenAPI
 * e a carga inicial de dados no banco H2.
 */
package senac.tsi.games.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

// @Configuration indica que esta classe fornece configurações ou beans que o Spring deve registrar no contexto.
@Configuration
// @OpenAPIDefinition define metadados globais da documentação Swagger/OpenAPI.
@OpenAPIDefinition(
        info = @Info(
                title = "Games API",
                version = "1.0.0",
                description = "API RESTful de jogos",
                contact = @Contact(
                        name = "Wanessa",
                        email = "wanessa@email.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
// A classe OpenApiConfig configura um aspecto estrutural da aplicação.
public class OpenApiConfig {
}