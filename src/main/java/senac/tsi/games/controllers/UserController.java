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
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.games.entities.User;
import senac.tsi.games.exceptions.SearchResultNotFoundException;
import senac.tsi.games.exceptions.UserNotFoundException;
import senac.tsi.games.repositories.UserRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "Users", description = "Usuários que avaliam jogos e podem possuir chaves de API. Relações: User -> Reviews (One-to-Many) e User -> ApiKeys (One-to-Many).")
@RestController
public class UserController {

    private final UserRepository userRepository;
    private final PagedResourcesAssembler<User> pagedAssembler;

    // Idempotency storage
    private final Map<String, IdempotentCreateResponse> createUserResponses = new ConcurrentHashMap<>();
    private final Object createUserIdempotencyLock = new Object();

    private record CreateUserFingerprint(String name, String email) {}
    private record IdempotentCreateResponse(CreateUserFingerprint requestFingerprint, User user, URI location) {}

    @Autowired
    public UserController(UserRepository userRepository,
                          PagedResourcesAssembler<User> pagedAssembler) {
        this.userRepository = userRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Listar usuários", description = "Retorna usuários paginados. Cada usuário pode ter várias reviews e várias chaves de API, ambas relações One-to-Many.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetros de paginação inválidos\"")))
    })
    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<User>>> getUsers(@ParameterObject Pageable pageable) {
        var page = userRepository.findAll(pageable);
        if (page.isEmpty()) {
            throw new SearchResultNotFoundException("Nenhum usuário encontrado para a página informada.");
        }
        PagedModel<EntityModel<User>> model = pagedAssembler.toModel(page,
                user -> EntityModel.of(user,
                        linkTo(methodOn(UserController.class).getUserById(user.getId())).withSelfRel(),
                        linkTo(methodOn(UserController.class).getUsers(pageable)).withRel("users"),
                        linkTo(methodOn(UserController.class).updateUser(user.getId(), null)).withRel("update"),
                        linkTo(methodOn(UserController.class).deleteUser(user.getId())).withRel("delete")));
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo ID, suas relações com Reviews e ApiKeys e links HATEOAS para navegação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find user 1\"")))
    })
    @GetMapping("/users/{id}")
    public EntityModel<User> getUserById(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable(name = "id") @NotNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return EntityModel.of(user,
                linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getUsers(Pageable.unpaged())).withRel("users"),
                linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"),
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"));
    }

    @Operation(summary = "Buscar usuários por email", description = "Consulta personalizada paginada. Busca usuários cujo email contenha o termo informado, ignorando maiúsculas/minúsculas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetro de busca inválido", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetro de busca inválido\"")))
    })
    @GetMapping("/users/search")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<User>>> searchUsersByEmail(
            @Parameter(description = "Email do usuário (busca parcial)", example = "wanessa", required = true)
            @RequestParam String email,
            @ParameterObject Pageable pageable) {
        var page = userRepository.findByEmailContainingIgnoreCase(email, pageable);
        if (page.isEmpty()) {
            throw new SearchResultNotFoundException("Nenhum usuário encontrado para o email " + email + " na página informada.");
        }
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                user -> EntityModel.of(user,
                        linkTo(methodOn(UserController.class).getUserById(user.getId())).withSelfRel(),
                        linkTo(methodOn(UserController.class).updateUser(user.getId(), null)).withRel("update"),
                        linkTo(methodOn(UserController.class).deleteUser(user.getId())).withRel("delete"))));
    }

    @Operation(summary = "Criar usuário", description = "Cria um usuário que poderá escrever reviews e gerar chaves de API. Requer X-API-Key e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "X-Idempotency-Key ausente ou em branco", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key header é obrigatório\""))),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key já utilizada com payload diferente\"")))
    })
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<User> createUser(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do usuário a ser criado", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"name\": \"Wanessa\", \"email\": \"wanessa@email.com\" }")))
            @Valid @RequestBody User newUser) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("X-Idempotency-Key header é obrigatório.");
        }

        var requestFingerprint = new CreateUserFingerprint(newUser.getName(), newUser.getEmail());

        synchronized (createUserIdempotencyLock) {
            var existing = createUserResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(existing.user());
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }

            var saved = userRepository.save(newUser);
            var location = URI.create("/users/" + saved.getId());
            createUserResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, saved, location));
            return ResponseEntity.created(location).body(saved);
        }
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza nome e email de um usuário. Reviews e ApiKeys vinculadas permanecem associadas ao mesmo ID.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Dados inválidos\""))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find user 1\"")))
    })
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do usuário", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"name\": \"Wanessa Silva\", \"email\": \"wanessa.silva@email.com\" }")))
            @Valid @RequestBody User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Operation(summary = "Deletar usuário", description = "Remove um usuário pelo ID. Como há cascade configurado, reviews e chaves de API vinculadas também podem ser removidas.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find user 1\"")))
    })
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) throw new UserNotFoundException(id);
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
