package senac.tsi.games.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import senac.tsi.games.entities.ApiKey;
import senac.tsi.games.entities.ApiKeyRole;
import senac.tsi.games.entities.User;
import senac.tsi.games.exceptions.ApiKeyNotFoundException;
import senac.tsi.games.exceptions.ConflictException;
import senac.tsi.games.exceptions.SearchResultNotFoundException;
import senac.tsi.games.exceptions.UserNotFoundException;
import senac.tsi.games.repositories.ApiKeyRepository;
import senac.tsi.games.repositories.UserRepository;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "API Keys", description = "Geração e gerenciamento de chaves de API. Relação: ApiKey -> User (Many-to-One), enquanto User -> ApiKeys é One-to-Many.")
@RestController
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PagedResourcesAssembler<ApiKey> pagedAssembler;

    private final Map<String, IdempotentCreateResponse> createApiKeyResponses = new ConcurrentHashMap<>();
    private final Object createApiKeyIdempotencyLock = new Object();

    public ApiKeyController(ApiKeyRepository apiKeyRepository,
                            UserRepository userRepository,
                            PagedResourcesAssembler<ApiKey> pagedAssembler) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.pagedAssembler = pagedAssembler;
    }

    public record CreateApiKeyRequest(
            @NotBlank @Size(max = 80) String label,
            ApiKeyRole role,
            @NotNull Long userId
    ) {
    }

    public record CreateUserApiKeyRequest(
            @NotBlank @Size(max = 80) String label,
            ApiKeyRole role
    ) {
    }

    public record UpdateApiKeyRequest(
            @NotBlank @Size(max = 80) String label,
            @NotNull ApiKeyRole role,
            @NotNull Boolean active,
            @NotNull Long userId
    ) {
    }

    private record CreateApiKeyFingerprint(String label, ApiKeyRole role, Long userId) {
    }

    private record IdempotentCreateResponse(CreateApiKeyFingerprint requestFingerprint, ApiKey apiKey, URI location) {
    }

    @Operation(summary = "Listar chaves de API", description = "Retorna chaves de API paginadas. Endpoint administrativo usado para gerenciar autenticação da API. Requer X-API-Key com nível ADMIN.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN")
    })
    @GetMapping("/api-keys")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> getApiKeys(@ParameterObject Pageable pageable) {
        var page = apiKeyRepository.findAll(pageable);
        if (page.isEmpty()) {
            throw new SearchResultNotFoundException("Nenhuma chave de API encontrada para a página informada.");
        }
        return ResponseEntity.ok(pagedAssembler.toModel(page, this::toModel));
    }

    @Operation(summary = "Buscar chave de API por ID", description = "Retorna uma chave de API específica, seu usuário dono e links HATEOAS para navegação, atualização e revogação. Requer X-API-Key com nível ADMIN.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chave encontrada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiKey.class))),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN"),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada")
    })
    @GetMapping("/api-keys/{id}")
    public EntityModel<ApiKey> getApiKeyById(
            @Parameter(description = "ID da chave", example = "1", required = true)
            @PathVariable Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        return toModel(apiKey);
    }

    @Operation(summary = "Buscar chaves por rótulo", description = "Consulta personalizada paginada. Busca chaves cujo rótulo contenha o texto informado, ignorando maiúsculas/minúsculas. Requer X-API-Key com nível ADMIN.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN")
    })
    @GetMapping("/api-keys/search")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> searchApiKeysByLabel(
            @Parameter(description = "Rótulo da chave", example = "Postman", required = true)
            @RequestParam String label,
            @ParameterObject Pageable pageable) {
        var page = apiKeyRepository.findByLabelContainingIgnoreCase(label, pageable);
        if (page.isEmpty()) {
            throw new SearchResultNotFoundException("Nenhuma chave de API encontrada para o rótulo " + label + " na página informada.");
        }
        return ResponseEntity.ok(pagedAssembler.toModel(page, this::toModel));
    }

    @Operation(summary = "Listar chaves de um usuário", description = "Lista, de forma paginada, as ApiKeys pertencentes a um User. Demonstra a relação One-to-Many de User para ApiKey. Requer X-API-Key com nível ADMIN.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chaves retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/users/{userId}/api-keys")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<ApiKey>>> getUserApiKeys(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long userId,
            @ParameterObject Pageable pageable) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        var page = apiKeyRepository.findByUserId(userId, pageable);
        if (page.isEmpty()) {
            throw new SearchResultNotFoundException("Nenhuma chave de API encontrada para o usuário " + userId + " na página informada.");
        }
        return ResponseEntity.ok(pagedAssembler.toModel(page, this::toModel));
    }

    @Operation(summary = "Criar chave de API", description = "Cria uma chave ativa para um User existente. A API gera automaticamente o valor da chave em keyValue. Esse valor deve ser enviado no header X-API-Key. Requer X-API-Key ADMIN e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chave criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiKey.class), examples = @ExampleObject(value = """
                    {
                      "label": "Postman",
                      "role": "WRITE",
                      "userId": 1
                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou X-Idempotency-Key ausente"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente")
    })
    @PostMapping("/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EntityModel<ApiKey>> createApiKey(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateApiKeyRequest request) {
        return createApiKey(idempotencyKey, request.label(), request.role(), request.userId());
    }

    @Operation(summary = "Gerar chave de API para um usuário", description = "Cria uma chave ativa usando o ID do usuário na rota. Mantém a navegação User -> ApiKeys. Requer chave ADMIN e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chave criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiKey.class), examples = @ExampleObject(value = """
                    {
                      "label": "Postman",
                      "role": "WRITE"
                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou X-Idempotency-Key ausente"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente")
    })
    @PostMapping("/users/{userId}/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EntityModel<ApiKey>> createApiKeyForUser(
            @Parameter(description = "ID do usuário dono da chave", example = "1", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateUserApiKeyRequest request) {
        return createApiKey(idempotencyKey, request.label(), request.role(), userId);
    }

    @Operation(summary = "Atualizar chave de API", description = "Atualiza rótulo, nível de acesso, status ativo e usuário dono da chave. Se o ID não existir, retorna 404. Requer X-API-Key com nível ADMIN.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chave atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiKey.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN"),
            @ApiResponse(responseCode = "404", description = "Chave ou usuário não encontrado")
    })
    @PutMapping("/api-keys/{id}")
    public ResponseEntity<EntityModel<ApiKey>> updateApiKey(
            @Parameter(description = "ID da chave", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        User user = userRepository.findById(request.userId()).orElseThrow(() -> new UserNotFoundException(request.userId()));

        apiKey.setLabel(request.label());
        apiKey.setRole(request.role());
        apiKey.setActive(request.active());
        apiKey.setUser(user);

        return ResponseEntity.ok(toModel(apiKeyRepository.save(apiKey)));
    }

    @Operation(summary = "Revogar chave de API", description = "Desativa uma ApiKey sem remover o registro do banco. A relação com User permanece para auditoria. Requer X-API-Key com nível ADMIN.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chave revogada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente"),
            @ApiResponse(responseCode = "403", description = "Chave sem permissão ADMIN"),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada")
    })
    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> revokeApiKey(
            @Parameter(description = "ID da chave", example = "1", required = true)
            @PathVariable Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<EntityModel<ApiKey>> createApiKey(String idempotencyKey, String label, ApiKeyRole requestedRole, Long userId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("X-Idempotency-Key header é obrigatório.");
        }

        ApiKeyRole role = requestedRole == null ? ApiKeyRole.WRITE : requestedRole;
        var requestFingerprint = new CreateApiKeyFingerprint(label, role, userId);

        synchronized (createApiKeyIdempotencyLock) {
            var existing = createApiKeyResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(toModel(existing.apiKey()));
                }
                throw new ConflictException("X-Idempotency-Key já utilizada com payload diferente.");
            }

            User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            ApiKey apiKey = apiKeyRepository.save(new ApiKey(
                    label,
                    "games_" + UUID.randomUUID(),
                    role,
                    user));
            URI location = URI.create("/api-keys/" + apiKey.getId());
            createApiKeyResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, apiKey, location));
            return ResponseEntity.created(location).body(toModel(apiKey));
        }
    }

    private EntityModel<ApiKey> toModel(ApiKey apiKey) {
        Long userId = apiKey.getUser() == null ? null : apiKey.getUser().getId();
        EntityModel<ApiKey> model = EntityModel.of(apiKey,
                linkTo(methodOn(ApiKeyController.class).getApiKeyById(apiKey.getId())).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).getApiKeys(Pageable.unpaged())).withRel("api-keys"),
                linkTo(methodOn(ApiKeyController.class).updateApiKey(apiKey.getId(), null)).withRel("update"),
                linkTo(methodOn(ApiKeyController.class).revokeApiKey(apiKey.getId())).withRel("delete"));
        if (userId != null) {
            model.add(linkTo(methodOn(UserController.class).getUserById(userId)).withRel("user"));
            model.add(linkTo(methodOn(ApiKeyController.class).getUserApiKeys(userId, Pageable.unpaged())).withRel("user-api-keys"));
        }
        return model;
    }
}
