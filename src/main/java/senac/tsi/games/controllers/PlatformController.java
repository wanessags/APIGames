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
import senac.tsi.games.entities.Platform;
import senac.tsi.games.exceptions.PlatformNotFoundException;
import senac.tsi.games.repositories.PlatformRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "Platforms", description = "Plataformas em que jogos podem ser publicados. Relação: Platform <-> Game (Many-to-Many). Exemplos: PC, PlayStation 5 e Xbox Series X.")
@RestController
public class PlatformController {

    private final PlatformRepository platformRepository;
    private final PagedResourcesAssembler<Platform> pagedAssembler;

    // Idempotency storage
    private final Map<String, IdempotentCreateResponse> createPlatformResponses = new ConcurrentHashMap<>();
    private final Object createPlatformIdempotencyLock = new Object();

    private record CreatePlatformFingerprint(String name) {}
    private record IdempotentCreateResponse(CreatePlatformFingerprint requestFingerprint, Platform platform, URI location) {}

    @Autowired
    public PlatformController(PlatformRepository platformRepository,
                              PagedResourcesAssembler<Platform> pagedAssembler) {
        this.platformRepository = platformRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Listar plataformas", description = "Retorna plataformas paginadas. Cada plataforma pode estar associada a vários jogos, e cada jogo pode estar em várias plataformas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetros de paginação inválidos\"")))
    })
    @GetMapping("/platforms")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Platform>>> getPlatforms(@ParameterObject Pageable pageable) {
        var page = platformRepository.findAll(pageable);
        PagedModel<EntityModel<Platform>> model = pagedAssembler.toModel(page,
                platform -> EntityModel.of(platform,
                        linkTo(methodOn(PlatformController.class).getPlatformById(platform.getId())).withSelfRel(),
                        linkTo(methodOn(PlatformController.class).getPlatforms(pageable)).withRel("platforms"),
                        linkTo(methodOn(PlatformController.class).updatePlatform(platform.getId(), null)).withRel("update"),
                        linkTo(methodOn(PlatformController.class).deletePlatform(platform.getId())).withRel("delete")));
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Buscar plataforma por ID", description = "Retorna uma plataforma específica e seus links HATEOAS. A relação com Game e Many-to-Many.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plataforma encontrada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Platform.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Plataforma não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find platform 1\"")))
    })
    @GetMapping("/platforms/{id}")
    public EntityModel<Platform> getPlatformById(
            @Parameter(description = "ID da plataforma", example = "1", required = true)
            @PathVariable(name = "id") @NotNull Long id) {
        Platform platform = platformRepository.findById(id)
                .orElseThrow(() -> new PlatformNotFoundException(id));
        return EntityModel.of(platform,
                linkTo(methodOn(PlatformController.class).getPlatformById(id)).withSelfRel(),
                linkTo(methodOn(PlatformController.class).getPlatforms(Pageable.unpaged())).withRel("platforms"),
                linkTo(methodOn(PlatformController.class).updatePlatform(id, null)).withRel("update"),
                linkTo(methodOn(PlatformController.class).deletePlatform(id)).withRel("delete"));
    }

    @Operation(summary = "Buscar plataformas por nome", description = "Consulta personalizada paginada por nome da plataforma, com busca parcial e sem diferenciar maiúsculas/minúsculas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Platform.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetro de busca inválido", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetro de busca inválido\"")))
    })
    @GetMapping("/platforms/search")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Platform>>> searchPlatformsByName(
            @Parameter(description = "Nome da plataforma (busca parcial)", example = "Play", required = true)
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        var page = platformRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                platform -> EntityModel.of(platform,
                        linkTo(methodOn(PlatformController.class).getPlatformById(platform.getId())).withSelfRel(),
                        linkTo(methodOn(PlatformController.class).updatePlatform(platform.getId(), null)).withRel("update"),
                        linkTo(methodOn(PlatformController.class).deletePlatform(platform.getId())).withRel("delete"))));
    }

    @Operation(summary = "Criar plataforma", description = "Cria uma plataforma que poderá ser vinculada a jogos no relacionamento Many-to-Many. Requer X-API-Key e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Plataforma criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Platform.class))),
            @ApiResponse(responseCode = "400", description = "X-Idempotency-Key ausente ou em branco", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key header é obrigatório\""))),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key já utilizada com payload diferente\"")))
    })
    @PostMapping("/platforms")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Platform> createPlatform(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da plataforma a ser criada", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"name\": \"PlayStation 5\" }")))
            @Valid @RequestBody Platform newPlatform) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var requestFingerprint = new CreatePlatformFingerprint(newPlatform.getName());

        synchronized (createPlatformIdempotencyLock) {
            var existing = createPlatformResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(existing.platform());
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }

            var saved = platformRepository.save(newPlatform);
            var location = URI.create("/platforms/" + saved.getId());
            createPlatformResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, saved, location));
            return ResponseEntity.created(location).body(saved);
        }
    }

    @Operation(summary = "Atualizar plataforma", description = "Atualiza os dados da plataforma e seus vínculos com jogos, mantendo a relação Many-to-Many.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plataforma atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Platform.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Dados inválidos\""))),
            @ApiResponse(responseCode = "404", description = "Plataforma não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find platform 1\"")))
    })
    @PutMapping("/platforms/{id}")
    public ResponseEntity<Platform> updatePlatform(
            @Parameter(description = "ID da plataforma", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados da plataforma", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"name\": \"Xbox Series X\" }")))
            @Valid @RequestBody Platform updatedPlatform) {
        return platformRepository.findById(id).map(platform -> {
            platform.setName(updatedPlatform.getName());
            platform.setGames(updatedPlatform.getGames());
            return ResponseEntity.ok(platformRepository.save(platform));
        }).orElseThrow(() -> new PlatformNotFoundException(id));
    }

    @Operation(summary = "Deletar plataforma", description = "Remove uma plataforma pelo ID. Requer X-API-Key válida.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Plataforma deletada com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Plataforma não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find platform 1\"")))
    })
    @DeleteMapping("/platforms/{id}")
    public ResponseEntity<Void> deletePlatform(
            @Parameter(description = "ID da plataforma", example = "1", required = true)
            @PathVariable Long id) {
        var platform = platformRepository.findById(id).orElse(null);
        if (platform == null) return ResponseEntity.notFound().build();
        platformRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
