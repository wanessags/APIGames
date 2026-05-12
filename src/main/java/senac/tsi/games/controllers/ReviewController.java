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
import senac.tsi.games.entities.Review;
import senac.tsi.games.entities.User;
import senac.tsi.games.exceptions.GameNotFoundException;
import senac.tsi.games.exceptions.ReviewNotFoundException;
import senac.tsi.games.exceptions.UserNotFoundException;
import senac.tsi.games.repositories.GameRepository;
import senac.tsi.games.repositories.ReviewRepository;
import senac.tsi.games.repositories.UserRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "Reviews", description = "Avaliações dos jogos. Relações: Review -> Game (Many-to-One) e Review -> User (Many-to-One). A nota deve ficar entre 0 e 10.")
@RestController
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final PagedResourcesAssembler<Review> pagedAssembler;

    // Idempotency storage
    private final Map<String, IdempotentCreateResponse> createReviewResponses = new ConcurrentHashMap<>();
    private final Object createReviewIdempotencyLock = new Object();

    private record CreateReviewFingerprint(String comment, Integer score, Long gameId, Long userId) {}
    private record IdempotentCreateResponse(CreateReviewFingerprint requestFingerprint, Review review, URI location) {}

    @Autowired
    public ReviewController(ReviewRepository reviewRepository,
                            GameRepository gameRepository,
                            UserRepository userRepository,
                            PagedResourcesAssembler<Review> pagedAssembler) {
        this.reviewRepository = reviewRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Listar reviews", description = "Retorna reviews paginadas. Cada review pertence a um jogo e a um usuário, representando duas relações Many-to-One.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetros de paginação inválidos\"")))
    })
    @GetMapping("/reviews")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Review>>> getReviews(@ParameterObject Pageable pageable) {
        var page = reviewRepository.findAll(pageable);
        PagedModel<EntityModel<Review>> model = pagedAssembler.toModel(page,
                review -> EntityModel.of(review,
                        linkTo(methodOn(ReviewController.class).getReviewById(review.getId())).withSelfRel(),
                        linkTo(methodOn(ReviewController.class).getReviews(pageable)).withRel("reviews"),
                        linkTo(methodOn(ReviewController.class).updateReview(review.getId(), null)).withRel("update"),
                        linkTo(methodOn(ReviewController.class).deleteReview(review.getId())).withRel("delete")));
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Buscar review por ID", description = "Retorna uma review específica e suas relações com Game e User, além dos links HATEOAS self, reviews, update e delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review encontrada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Review não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find review 1\"")))
    })
    @GetMapping("/reviews/{id}")
    public EntityModel<Review> getReviewById(
            @Parameter(description = "ID da review", example = "1", required = true)
            @PathVariable(name = "id") @NotNull Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException(id));
        return EntityModel.of(review,
                linkTo(methodOn(ReviewController.class).getReviewById(id)).withSelfRel(),
                linkTo(methodOn(ReviewController.class).getReviews(Pageable.unpaged())).withRel("reviews"),
                linkTo(methodOn(ReviewController.class).updateReview(id, null)).withRel("update"),
                linkTo(methodOn(ReviewController.class).deleteReview(id)).withRel("delete"));
    }

    @Operation(summary = "Buscar reviews de um jogo", description = "Consulta personalizada paginada. Recebe o ID de um Game e retorna as Reviews vinculadas a ele pela relação One-to-Many do lado de Game.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews encontradas com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\"")))
    })
    @GetMapping("/games/{id}/reviews")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Review>>> getReviewsByGame(
            @Parameter(description = "ID do jogo", example = "1", required = true)
            @PathVariable Long id,
            @ParameterObject Pageable pageable) {
        gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
        var page = reviewRepository.findByGameId(id, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                review -> EntityModel.of(review,
                        linkTo(methodOn(ReviewController.class).getReviewById(review.getId())).withSelfRel(),
                        linkTo(methodOn(ReviewController.class).updateReview(review.getId(), null)).withRel("update"),
                        linkTo(methodOn(ReviewController.class).deleteReview(review.getId())).withRel("delete"))));
    }

    @Operation(summary = "Buscar media das notas de um jogo", description = "Consulta personalizada agregada. Recebe o ID de um Game e calcula a media das notas das Reviews associadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Média retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Jogo não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\"")))
    })
    @GetMapping("/games/{id}/rating")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Object>> getAverageScoreByGame(
            @Parameter(description = "ID do jogo", example = "1", required = true)
            @PathVariable Long id) {
        gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
        Double average = reviewRepository.getAverageScoreByGameId(id);
        return ResponseEntity.ok(Map.of("gameId", id, "averageScore", average == null ? 0.0 : average));
    }

    @Operation(summary = "Criar review", description = "Cria uma review para um Game existente e um User existente. O corpo deve enviar game.id e user.id. Requer X-API-Key e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class))),
            @ApiResponse(responseCode = "400", description = "X-Idempotency-Key ausente ou em branco", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key header é obrigatório\""))),
            @ApiResponse(responseCode = "404", description = "Jogo ou usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find game 1\""))),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key já utilizada com payload diferente\"")))
    })
    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Review> createReview(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da review a ser criada", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "comment": "Jogo incrível, recomendo!",
                                      "score": 9,
                                      "game": { "id": 1 },
                                      "user": { "id": 1 }
                                    }""")))
            @Valid @RequestBody Review newReview) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Long gameId = getGameId(newReview.getGame());
        Long userId = getUserId(newReview.getUser());

        var requestFingerprint = new CreateReviewFingerprint(
                newReview.getComment(),
                newReview.getScore(),
                gameId,
                userId
        );

        synchronized (createReviewIdempotencyLock) {
            var existing = createReviewResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(existing.review());
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }

            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new GameNotFoundException(gameId));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            newReview.setGame(game);
            newReview.setUser(user);

            Review saved = reviewRepository.save(newReview);
            var location = URI.create("/reviews/" + saved.getId());
            createReviewResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, saved, location));
            return ResponseEntity.created(location).body(saved);
        }
    }

    @Operation(summary = "Atualizar review", description = "Atualiza comentario, nota e os vínculos da review com Game e User.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Dados inválidos\""))),
            @ApiResponse(responseCode = "404", description = "Jogo, usuário ou review não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find review 1\"")))
    })
    @PutMapping("/reviews/{id}")
    public ResponseEntity<Review> updateReview(
            @Parameter(description = "ID da review", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados da review", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "comment": "Melhor jogo que já joguei!",
                                      "score": 10,
                                      "game": { "id": 1 },
                                      "user": { "id": 1 }
                                    }""")))
            @Valid @RequestBody Review updatedReview) {
        Long gameId = getGameId(updatedReview.getGame());
        Long userId = getUserId(updatedReview.getUser());
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return reviewRepository.findById(id).map(review -> {
            review.setComment(updatedReview.getComment());
            review.setScore(updatedReview.getScore());
            review.setGame(game);
            review.setUser(user);
            return ResponseEntity.ok(reviewRepository.save(review));
        }).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    @Operation(summary = "Deletar review", description = "Remove uma review pelo ID sem remover o jogo nem o usuário relacionados.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review deletada com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Review não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find review 1\"")))
    })
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "ID da review", example = "1", required = true)
            @PathVariable Long id) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return ResponseEntity.notFound().build();
        reviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }

    private Long getGameId(Game game) {
        if (game == null || game.getId() == null) {
            throw new IllegalArgumentException("game.id é obrigatório.");
        }
        return game.getId();
    }

    private Long getUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("user.id é obrigatório.");
        }
        return user.getId();
    }
}
