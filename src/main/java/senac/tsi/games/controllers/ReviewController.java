
package senac.tsi.games.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import senac.tsi.games.entities.Review;
import senac.tsi.games.entities.User;
import senac.tsi.games.exceptions.GameNotFoundException;
import senac.tsi.games.exceptions.ReviewNotFoundException;
import senac.tsi.games.exceptions.UserNotFoundException;
import senac.tsi.games.repositories.GameRepository;
import senac.tsi.games.repositories.ReviewRepository;
import senac.tsi.games.repositories.UserRepository;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// @Tag agrupa e organiza os endpoints no Swagger, facilitando a leitura da documentação.
@Tag(name = "reviews", description = "Reviews route")
@RestController
// A classe ReviewController expõe os endpoints HTTP dessa entidade e organiza o comportamento REST da API.
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final PagedResourcesAssembler<Review> pagedAssembler;

    // @Autowired aqui reforça que o Spring deve fornecer automaticamente as dependências no construtor.
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

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get all reviews")
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/reviews")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
// Este método atende uma operação de leitura e devolve dados de ReviewController pela API.
// Aqui a resposta usa paginação e HATEOAS ao mesmo tempo: por isso o retorno é mais rico do que uma simples List.
    public ResponseEntity<PagedModel<EntityModel<Review>>> getReviews(@ParameterObject Pageable pageable) {

        var page = reviewRepository.findAll(pageable);

// O pagedAssembler transforma a página retornada pelo JPA em um modelo HATEOAS paginado, com links embutidos.
        PagedModel<EntityModel<Review>> model = pagedAssembler.toModel(
                page,
// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
                review -> EntityModel.of(review,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(ReviewController.class).getReviewById(review.getId())).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(ReviewController.class).getReviews(pageable)).withRel("reviews"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(ReviewController.class).updateReview(review.getId(), null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(ReviewController.class).deleteReview(review.getId())).withRel("delete"))
        );

// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(model);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get review by id")
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/reviews/{id}")
// Este método atende uma operação de leitura e devolve dados de ReviewController pela API.
    public EntityModel<Review> getReviewById(
            @PathVariable(name = "id", required = true)
            @NotNull Long id) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        Review review = reviewRepository.findById(id)
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new ReviewNotFoundException(id));

// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
        return EntityModel.of(review,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(ReviewController.class).getReviewById(id)).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(ReviewController.class).getReviews(Pageable.unpaged())).withRel("reviews"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(ReviewController.class).updateReview(id, null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(ReviewController.class).deleteReview(id)).withRel("delete"));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Search reviews by score")
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/reviews/search")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Review>> searchReviewsByScore(@RequestParam Integer score) {
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(reviewRepository.findByScore(score));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Create review")
// @PostMapping marca um endpoint de criação, usado quando queremos inserir um novo recurso.
    @PostMapping("/reviews")
// O status 201 indica que um novo recurso foi criado com sucesso.
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Review> createReview(@RequestBody Review newReview) {

        Game game = gameRepository.findById(newReview.getGame().getId())
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new GameNotFoundException(newReview.getGame().getId()));

        User user = userRepository.findById(newReview.getUser().getId())
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new UserNotFoundException(newReview.getUser().getId()));

        newReview.setGame(game);
        newReview.setUser(user);

        Review savedReview = reviewRepository.save(newReview);

        user.setReview(savedReview);
        userRepository.save(user);

// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
        return ResponseEntity.created(
                        URI.create("/reviews/" + savedReview.getId()))
                .body(savedReview);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Update review")
// @PutMapping marca um endpoint de atualização, normalmente usado para alterar um recurso já existente.
    @PutMapping("/reviews/{id}")
    public ResponseEntity<Review> updateReview(@PathVariable Long id,
                                               @RequestBody Review updatedReview) {

        Game game = gameRepository.findById(updatedReview.getGame().getId())
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new GameNotFoundException(updatedReview.getGame().getId()));

        User user = userRepository.findById(updatedReview.getUser().getId())
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new UserNotFoundException(updatedReview.getUser().getId()));

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        return reviewRepository.findById(id).map(
                review -> {
// Antes de apagar a review, o código desfaz a ligação com User para não deixar referência pendurada e evitar erro do Hibernate.
                    if (review.getUser() != null && !review.getUser().getId().equals(user.getId())) {
                        User oldUser = review.getUser();
                        oldUser.setReview(null);
                        userRepository.save(oldUser);
                    }

                    review.setComment(updatedReview.getComment());
                    review.setScore(updatedReview.getScore());
                    review.setGame(game);
                    review.setUser(user);

                    Review savedReview = reviewRepository.save(review);

                    user.setReview(savedReview);
                    userRepository.save(user);

// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
                    return ResponseEntity.ok(savedReview);
                }
        ).orElseGet(() -> {
            updatedReview.setGame(game);
            updatedReview.setUser(user);

            Review savedReview = reviewRepository.save(updatedReview);

            user.setReview(savedReview);
            userRepository.save(user);

// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
            return ResponseEntity.created(
                            URI.create("/reviews/" + savedReview.getId()))
                    .body(savedReview);
        });
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Delete review")
// @DeleteMapping marca um endpoint de remoção de recurso.
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        var review = reviewRepository.findById(id).orElse(null);

        if (review == null) {
// notFound devolve 404 sem corpo quando o recurso pedido para remoção não existe.
            return ResponseEntity.notFound().build();
        }

// Antes de apagar a review, o código desfaz a ligação com User para não deixar referência pendurada e evitar erro do Hibernate.
        if (review.getUser() != null) {
            User user = review.getUser();
// Aqui a referência do usuário para a review é limpa manualmente, o que mantém a integridade do relacionamento OneToOne.
            user.setReview(null);
            userRepository.save(user);
        }

// Depois de desfazer os vínculos necessários, a review pode ser removida com segurança do banco.
        reviewRepository.delete(review);
// noContent devolve 204, que é o status correto quando a exclusão funciona e não há corpo de resposta.
        return ResponseEntity.noContent().build();
    }
}