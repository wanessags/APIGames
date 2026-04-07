
package senac.tsi.games.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// @Tag agrupa e organiza os endpoints no Swagger, facilitando a leitura da documentação.
@Tag(name = "games", description = "Games route")
@RestController
// A classe GameController expõe os endpoints HTTP dessa entidade e organiza o comportamento REST da API.
public class GameController {

    private final GameRepository gameRepository;
    private final PagedResourcesAssembler<Game> pagedAssembler;

    // @Autowired aqui reforça que o Spring deve fornecer automaticamente as dependências no construtor.
    @Autowired
    public GameController(GameRepository gameRepository,
                          PagedResourcesAssembler<Game> pagedAssembler) {
        this.gameRepository = gameRepository;
        this.pagedAssembler = pagedAssembler;
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get all games", description = "Get all games with pagination and HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Games found successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/games")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
// Este método atende uma operação de leitura e devolve dados de GameController pela API.
// Aqui a resposta usa paginação e HATEOAS ao mesmo tempo: por isso o retorno é mais rico do que uma simples List.
    public ResponseEntity<PagedModel<EntityModel<Game>>> getGames(@ParameterObject Pageable pageable) {

        var page = gameRepository.findAll(pageable);

// O pagedAssembler transforma a página retornada pelo JPA em um modelo HATEOAS paginado, com links embutidos.
        PagedModel<EntityModel<Game>> model = pagedAssembler.toModel(
                page,
// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
                game -> EntityModel.of(game,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(GameController.class).getGameById(game.getId())).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(GameController.class).getGames(pageable)).withRel("games"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(GameController.class).updateGame(game.getId(), null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(GameController.class).deleteGame(game.getId())).withRel("delete"))
        );

// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(model);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get game by id", description = "Get one game by id with HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Game found",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Game.class)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid id supplied"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/games/{id}")
// Este método atende uma operação de leitura e devolve dados de GameController pela API.
    public EntityModel<Game> getGameById(
            @PathVariable(name = "id", required = true)
            @NotNull Long id) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        Game game = gameRepository.findById(id)
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new GameNotFoundException(id));

// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
        return EntityModel.of(game,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(GameController.class).getGameById(id)).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(GameController.class).getGames(Pageable.unpaged())).withRel("games"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(GameController.class).updateGame(id, null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(GameController.class).deleteGame(id)).withRel("delete"));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Search games by name", description = "Search games filtering by name")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Search executed successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/games/search")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Game>> searchGamesByName(@RequestParam String name) {
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(gameRepository.findByNameContainingIgnoreCase(name));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Create game", description = "Create a new game")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "Game created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Game.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "Cyberpunk 2077",
                                      "price": 249.90
                                    }
                                    """)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PostMapping marca um endpoint de criação, usado quando queremos inserir um novo recurso.
    @PostMapping("/games")
// O status 201 indica que um novo recurso foi criado com sucesso.
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Game> createGame(@RequestBody Game newGame) {
        Game savedGame = gameRepository.save(newGame);

// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
        return ResponseEntity.created(
                        URI.create("/games/" + savedGame.getId()))
                .body(savedGame);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Update game", description = "Update an existing game")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Game updated successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "Game created successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PutMapping marca um endpoint de atualização, normalmente usado para alterar um recurso já existente.
    @PutMapping("/games/{id}")
    public ResponseEntity<Game> updateGame(@PathVariable Long id,
                                           @RequestBody Game updatedGame) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        return gameRepository.findById(id).map(
                game -> {
                    game.setName(updatedGame.getName());
                    game.setPrice(updatedGame.getPrice());
                    game.setCategory(updatedGame.getCategory());
                    game.setPlatforms(updatedGame.getPlatforms());
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
                    return ResponseEntity.ok(gameRepository.save(game));
                }
        ).orElseGet(() -> {
            Game savedGame = gameRepository.save(updatedGame);
// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
            return ResponseEntity.created(
                            URI.create("/games/" + savedGame.getId()))
                    .body(savedGame);
        });
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Delete game", description = "Delete a game by id")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "204", description = "Game deleted successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
// @DeleteMapping marca um endpoint de remoção de recurso.
    @DeleteMapping("/games/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        var game = gameRepository.findById(id).orElse(null);

        if (game == null) {
// notFound devolve 404 sem corpo quando o recurso pedido para remoção não existe.
            return ResponseEntity.notFound().build();
        }

        gameRepository.deleteById(id);
// noContent devolve 204, que é o status correto quando a exclusão funciona e não há corpo de resposta.
        return ResponseEntity.noContent().build();
    }
}