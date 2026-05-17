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
import senac.tsi.games.entities.Game;
import senac.tsi.games.entities.GameDetail;
import senac.tsi.games.exceptions.ConflictException;
import senac.tsi.games.exceptions.GameDetailNotFoundException;
import senac.tsi.games.exceptions.GameNotFoundException;
import senac.tsi.games.exceptions.SearchResultNotFoundException;
import senac.tsi.games.repositories.GameDetailRepository;
import senac.tsi.games.repositories.GameRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "GameDetails", description = "Detalhes complementares do jogo. Relação: GameDetail -> Game (One-to-One). Cada jogo deve ter no máximo um detalhe.")
@RestController
public class GameDetailController {

    private final GameDetailRepository gameDetailRepository;
    private final GameRepository gameRepository;
    private final PagedResourcesAssembler<GameDetail> pagedAssembler;

    // Idempotency storage
    private final Map<String, IdempotentCreateResponse> createDetailResponses = new ConcurrentHashMap<>();
    private final Object createDetailIdempotencyLock = new Object();

    private record CreateDetailFingerprint(String description, String developer, Long gameId) {}
    private record IdempotentCreateResponse(CreateDetailFingerprint requestFingerprint, GameDetail detail, URI location) {}

    @Autowired
    public GameDetailController(GameDetailRepository gameDetailRepository,
                                GameRepository gameRepository,
                                PagedResourcesAssembler<GameDetail> pagedAssembler) {
        this.gameDetailRepository = gameDetailRepository;
        this.gameRepository = gameRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Listar detalhes de jogos", description = "Retorna detalhes paginados. Cada item representa informações complementares de exatamente um jogo, usando relacionamento One-to-One.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetros de paginação inválidos\"")))
    })
    @GetMapping("/game-details")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<GameDetail>>> getGameDetails(@ParameterObject Pageable pageable) {
        var page = gameDetailRepository.findAll(pageable);
        if (page.isEmpty()) {
            throw new SearchResultNotFoundException("Nenhum detalhe de jogo encontrado para a página informada.");
        }
        PagedModel<EntityModel<GameDetail>> model = pagedAssembler.toModel(page,
                detail -> EntityModel.of(detail,
                        linkTo(methodOn(GameDetailController.class).getGameDetailById(detail.getId())).withSelfRel(),
                        linkTo(methodOn(GameDetailController.class).getGameDetails(pageable)).withRel("game-details"),
                        linkTo(methodOn(GameDetailController.class).updateGameDetail(detail.getId(), null)).withRel("update"),
                        linkTo(methodOn(GameDetailController.class).deleteGameDetail(detail.getId())).withRel("delete")));
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Buscar detalhe por ID", description = "Retorna o detalhe de um jogo pelo ID do detalhe, com links HATEOAS para consultar, atualizar ou excluir o recurso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhe encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDetail.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Detalhe não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game detail 1\"")))
    })
    @GetMapping("/game-details/{id}")
    public EntityModel<GameDetail> getGameDetailById(
            @Parameter(description = "ID do detalhe", example = "1", required = true)
            @PathVariable(name = "id") @NotNull Long id) {
        GameDetail detail = gameDetailRepository.findById(id)
                .orElseThrow(() -> new GameDetailNotFoundException(id));
        return EntityModel.of(detail,
                linkTo(methodOn(GameDetailController.class).getGameDetailById(id)).withSelfRel(),
                linkTo(methodOn(GameDetailController.class).getGameDetails(Pageable.unpaged())).withRel("game-details"),
                linkTo(methodOn(GameDetailController.class).updateGameDetail(id, null)).withRel("update"),
                linkTo(methodOn(GameDetailController.class).deleteGameDetail(id)).withRel("delete"));
    }

    @Operation(summary = "Buscar detalhe pelo ID do jogo", description = "Consulta personalizada do relacionamento One-to-One. Recebe o ID de um Game e retorna o GameDetail associado a ele.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhe encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDetail.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Jogo ou detalhe não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game detail 1\"")))
    })
    @GetMapping("/games/{id}/detail")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<EntityModel<GameDetail>> getDetailByGame(
            @Parameter(description = "ID do jogo", example = "1", required = true)
            @PathVariable Long id) {
        gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
        GameDetail detail = gameDetailRepository.findByGameId(id)
                .orElseThrow(() -> new GameDetailNotFoundException(id));
        return ResponseEntity.ok(EntityModel.of(detail,
                linkTo(methodOn(GameDetailController.class).getGameDetailById(detail.getId())).withSelfRel(),
                linkTo(methodOn(GameDetailController.class).getGameDetails(Pageable.unpaged())).withRel("game-details"),
                linkTo(methodOn(GameDetailController.class).updateGameDetail(detail.getId(), null)).withRel("update"),
                linkTo(methodOn(GameDetailController.class).deleteGameDetail(detail.getId())).withRel("delete")));
    }

    @Operation(summary = "Criar detalhe de jogo", description = "Cria o detalhe de um jogo existente. O corpo deve enviar game.id para formar a relação One-to-One. Requer X-API-Key e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Detalhe criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDetail.class))),
            @ApiResponse(responseCode = "400", description = "X-Idempotency-Key ausente ou em branco", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key header é obrigatório\""))),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\""))),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key já utilizada com payload diferente\"")))
    })
    @PostMapping("/game-details")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<GameDetail> createGameDetail(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do detalhe a ser criado", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "description": "Jogo de mundo aberto com missões épicas",
                                      "developer": "Rockstar Games",
                                      "game": { "id": 1 }
                                    }""")))
            @Valid @RequestBody GameDetail newDetail) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("X-Idempotency-Key header é obrigatório.");
        }

        Long gameId = getGameId(newDetail);

        var requestFingerprint = new CreateDetailFingerprint(
                newDetail.getDescription(),
                newDetail.getDeveloper(),
                gameId
        );

        synchronized (createDetailIdempotencyLock) {
            var existing = createDetailResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(existing.detail());
                } else {
                    throw new ConflictException("X-Idempotency-Key já utilizada com payload diferente.");
                }
            }

            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new GameNotFoundException(gameId));
            newDetail.setGame(game);

            GameDetail saved = gameDetailRepository.save(newDetail);
            var location = URI.create("/game-details/" + saved.getId());
            createDetailResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, saved, location));
            return ResponseEntity.created(location).body(saved);
        }
    }

    @Operation(summary = "Atualizar detalhe de jogo", description = "Atualiza descrição e desenvolvedora do detalhe. A associação com Game representa o relacionamento One-to-One.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhe atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDetail.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Dados inválidos\""))),
            @ApiResponse(responseCode = "404", description = "Detalhe não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game detail 1\"")))
    })
    @PutMapping("/game-details/{id}")
    public ResponseEntity<GameDetail> updateGameDetail(
            @Parameter(description = "ID do detalhe", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do detalhe", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "description": "RPG de fantasia com mundo enorme",
                                      "developer": "CD Projekt Red"
                                    }""")))
            @Valid @RequestBody GameDetail updatedDetail) {
        return gameDetailRepository.findById(id).map(detail -> {
            detail.setDescription(updatedDetail.getDescription());
            detail.setDeveloper(updatedDetail.getDeveloper());
            return ResponseEntity.ok(gameDetailRepository.save(detail));
        }).orElseThrow(() -> new GameDetailNotFoundException(id));
    }

    @Operation(summary = "Deletar detalhe de jogo", description = "Remove o detalhe pelo ID sem remover diretamente o jogo principal.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Detalhe deletado com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Detalhe não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game detail 1\"")))
    })
    @DeleteMapping("/game-details/{id}")
    public ResponseEntity<Void> deleteGameDetail(
            @Parameter(description = "ID do detalhe", example = "1", required = true)
            @PathVariable Long id) {
        GameDetail detail = gameDetailRepository.findById(id).orElse(null);
        if (detail == null) throw new GameDetailNotFoundException(id);
        gameDetailRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Long getGameId(GameDetail detail) {
        if (detail.getGame() == null || detail.getGame().getId() == null) {
            throw new IllegalArgumentException("game.id é obrigatório.");
        }
        return detail.getGame().getId();
    }
}
