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
import senac.tsi.games.exceptions.GameNotFoundException;
import senac.tsi.games.repositories.GameRepository;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "Games", description = "Jogos são o recurso principal da API. Relações: Game -> Category (Many-to-One), Game -> Platforms (Many-to-Many), Game -> Reviews (One-to-Many) e Game -> GameDetail (One-to-One).")
@RestController
public class GameController {

    private final GameRepository gameRepository;
    private final PagedResourcesAssembler<Game> pagedAssembler;

    // Idempotency storage
    private final Map<String, IdempotentCreateResponse> createGameResponses = new ConcurrentHashMap<>();
    private final Object createGameIdempotencyLock = new Object();

    private record CreateGameFingerprint(String name, Double price, Long categoryId, List<Long> platformIds) {}
    private record IdempotentCreateResponse(CreateGameFingerprint requestFingerprint, Game game, URI location) {}

    @Autowired
    public GameController(GameRepository gameRepository,
                          PagedResourcesAssembler<Game> pagedAssembler) {
        this.gameRepository = gameRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Listar jogos", description = "Retorna uma página de jogos com links HATEOAS. Use este endpoint para visualizar o catálogo principal e navegar para categoria, plataformas, reviews e detalhe de cada jogo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetros de paginação inválidos\"")))
    })
    @GetMapping("/games")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Game>>> getGames(@ParameterObject Pageable pageable) {
        var page = gameRepository.findAll(pageable);
        PagedModel<EntityModel<Game>> model = pagedAssembler.toModel(page,
                game -> EntityModel.of(game,
                        linkTo(methodOn(GameController.class).getGameById(game.getId())).withSelfRel(),
                        linkTo(methodOn(GameController.class).getGames(pageable)).withRel("games"),
                        linkTo(methodOn(GameController.class).updateGame(game.getId(), null)).withRel("update"),
                        linkTo(methodOn(GameController.class).deleteGame(game.getId())).withRel("delete")));
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Buscar jogo por ID", description = "Retorna um jogo específico pelo ID. O recurso mostra as relações do jogo com Category, Platforms, Reviews e GameDetail, além dos links HATEOAS self, games, update e delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jogo encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\"")))
    })
    @GetMapping("/games/{id}")
    public EntityModel<Game> getGameById(
            @Parameter(description = "ID do jogo", example = "1", required = true)
            @PathVariable(name = "id") @NotNull Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
        return EntityModel.of(game,
                linkTo(methodOn(GameController.class).getGameById(id)).withSelfRel(),
                linkTo(methodOn(GameController.class).getGames(Pageable.unpaged())).withRel("games"),
                linkTo(methodOn(GameController.class).updateGame(id, null)).withRel("update"),
                linkTo(methodOn(GameController.class).deleteGame(id)).withRel("delete"));
    }

    @Operation(summary = "Buscar jogos por nome", description = "Consulta personalizada paginada. Retorna jogos cujo nome contenha o termo informado, ignorando maiúsculas/minúsculas. Mantém os links HATEOAS de navegação e manutenção.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetro de busca inválido", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetro de busca inválido\"")))
    })
    @GetMapping("/games/search")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Game>>> searchGamesByName(
            @Parameter(description = "Nome do jogo (busca parcial)", example = "GTA", required = true)
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        var page = gameRepository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                game -> EntityModel.of(game,
                        linkTo(methodOn(GameController.class).getGameById(game.getId())).withSelfRel(),
                        linkTo(methodOn(GameController.class).updateGame(game.getId(), null)).withRel("update"),
                        linkTo(methodOn(GameController.class).deleteGame(game.getId())).withRel("delete"))));
    }

    @Operation(summary = "Resumo do jogo - versão 1", description = "Endpoint de demonstração de versionamento por header. Envie X-API-Version: 1 para receber apenas id e nome do jogo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo v1 retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado")
    })
    @GetMapping(value = "/games/{id}/summary", headers = "X-API-Version=1")
    public EntityModel<Map<String, Object>> getGameSummaryV1(
            @Parameter(description = "ID do jogo", example = "1")
            @PathVariable Long id) {
        Game game = gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
        return EntityModel.of(Map.of(
                        "id", game.getId(),
                        "name", game.getName()),
                linkTo(methodOn(GameController.class).getGameById(id)).withRel("game"));
    }

    @Operation(summary = "Resumo do jogo - versão 2", description = "Endpoint de demonstração de versionamento por header. Envie X-API-Version: 2 para receber id, nome, preço, categoria, plataformas e quantidade de reviews.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo v2 retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado")
    })
    @GetMapping(value = "/games/{id}/summary", headers = "X-API-Version=2")
    public EntityModel<Map<String, Object>> getGameSummaryV2(
            @Parameter(description = "ID do jogo", example = "1")
            @PathVariable Long id) {
        Game game = gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
        String category = game.getCategory() == null ? null : game.getCategory().getType().name();
        List<String> platforms = game.getPlatforms().stream().map(platform -> platform.getName()).toList();
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("id", game.getId());
        summary.put("name", game.getName());
        summary.put("price", game.getPrice());
        summary.put("category", category);
        summary.put("platforms", platforms);
        summary.put("reviewCount", game.getReviews().size());
        return EntityModel.of(summary,
                linkTo(methodOn(GameController.class).getGameById(id)).withRel("game"),
                linkTo(methodOn(GameDetailController.class).getDetailByGame(id)).withRel("detail"),
                linkTo(methodOn(ReviewController.class).getReviewsByGame(id, Pageable.unpaged())).withRel("reviews"));
    }

    @Operation(summary = "Criar jogo", description = "Cria um novo jogo associado a uma Category existente e, opcionalmente, a Platforms existentes. Requer X-API-Key e X-Idempotency-Key. Se a mesma chave idempotente for repetida com o mesmo payload, a API devolve a mesma criação; se o payload mudar, retorna 409.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Jogo criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "400", description = "X-Idempotency-Key ausente ou em branco", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key header é obrigatório\""))),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key já utilizada com payload diferente\"")))
    })
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Game> createGame(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do jogo a ser criado", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "The Last of Us",
                                      "price": 299.90,
                                      "category": { "id": 1 },
                                      "platforms": [ { "id": 1 }, { "id": 2 } ]
                                    }""")))
            @Valid @RequestBody Game newGame) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var requestFingerprint = new CreateGameFingerprint(
                newGame.getName(),
                newGame.getPrice(),
                newGame.getCategory() == null ? null : newGame.getCategory().getId(),
                newGame.getPlatforms() == null
                        ? List.of()
                        : newGame.getPlatforms().stream().map(platform -> platform.getId()).filter(java.util.Objects::nonNull).sorted().toList());

        synchronized (createGameIdempotencyLock) {
            var existing = createGameResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(existing.game());
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }

            var saved = persistNewGame(newGame);
            var location = URI.create("/games/" + saved.getBody().getId());
            createGameResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, saved.getBody(), location));
            return saved;
        }
    }

    private ResponseEntity<Game> persistNewGame(Game newGame) {
        var savedGame = gameRepository.save(newGame);
        return ResponseEntity.created(URI.create("/games/" + savedGame.getId())).body(savedGame);
    }

    @Operation(summary = "Atualizar jogo", description = "Atualiza nome, preço, categoria e plataformas de um jogo. A relação com Reviews e GameDetail é mantida separadamente pelos endpoints específicos.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jogo atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Dados inválidos\""))),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\"")))
    })
    @PutMapping("/games/{id}")
    public ResponseEntity<Game> updateGame(
            @Parameter(description = "ID do jogo", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados do jogo", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "GTA V Updated",
                                      "price": 149.90,
                                      "category": { "id": 2 },
                                      "platforms": [ { "id": 1 } ]
                                    }""")))
            @Valid @RequestBody Game updatedGame) {
        return gameRepository.findById(id).map(game -> {
            game.setName(updatedGame.getName());
            game.setPrice(updatedGame.getPrice());
            game.setCategory(updatedGame.getCategory());
            game.setPlatforms(updatedGame.getPlatforms());
            return ResponseEntity.ok(gameRepository.save(game));
        }).orElseThrow(() -> new GameNotFoundException(id));
    }

    @Operation(summary = "Deletar jogo", description = "Remove um jogo pelo ID. Como há cascade configurado, reviews e detalhe vinculados ao jogo também podem ser removidos.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Jogo deletado com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\"")))
    })
    @DeleteMapping("/games/{id}")
    public ResponseEntity<Void> deleteGame(
            @Parameter(description = "ID do jogo", example = "1", required = true)
            @PathVariable Long id) {
        var game = gameRepository.findById(id).orElse(null);
        if (game == null) return ResponseEntity.notFound().build();
        gameRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
