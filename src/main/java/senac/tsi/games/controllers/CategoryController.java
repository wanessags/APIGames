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
import senac.tsi.games.entities.Category;
import senac.tsi.games.entities.CategoryType;
import senac.tsi.games.exceptions.CategoryNotFoundException;
import senac.tsi.games.repositories.CategoryRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "Categories", description = "Categorias classificam jogos por tipo. Relação: Category -> Games (One-to-Many). Tipos válidos: RPG, ACTION, ADVENTURE, SPORTS e STRATEGY.")
@RestController
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final PagedResourcesAssembler<Category> pagedAssembler;

    // Idempotency storage
    private final Map<String, IdempotentCreateResponse> createCategoryResponses = new ConcurrentHashMap<>();
    private final Object createCategoryIdempotencyLock = new Object();

    private record CreateCategoryFingerprint(String type) {}
    private record IdempotentCreateResponse(CreateCategoryFingerprint requestFingerprint, Category category, URI location) {}

    @Autowired
    public CategoryController(CategoryRepository categoryRepository,
                              PagedResourcesAssembler<Category> pagedAssembler) {
        this.categoryRepository = categoryRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Listar categorias", description = "Retorna categorias paginadas. Cada categoria pode agrupar vários jogos, representando a relação One-to-Many com Game.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Parâmetros de paginação inválidos\"")))
    })
    @GetMapping("/categories")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Category>>> getCategories(@ParameterObject Pageable pageable) {
        var page = categoryRepository.findAll(pageable);
        PagedModel<EntityModel<Category>> model = pagedAssembler.toModel(page,
                category -> EntityModel.of(category,
                        linkTo(methodOn(CategoryController.class).getCategoryById(category.getId())).withSelfRel(),
                        linkTo(methodOn(CategoryController.class).getCategories(pageable)).withRel("categories"),
                        linkTo(methodOn(CategoryController.class).updateCategory(category.getId(), null)).withRel("update"),
                        linkTo(methodOn(CategoryController.class).deleteCategory(category.getId())).withRel("delete")));
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Buscar categoria por ID", description = "Retorna uma categoria específica e seus links HATEOAS. A categoria é usada pelos jogos por meio de relação Many-to-One no lado de Game.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoria encontrada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find category 1\"")))
    })
    @GetMapping("/categories/{id}")
    public EntityModel<Category> getCategoryById(
            @Parameter(description = "ID da categoria", example = "1", required = true)
            @PathVariable(name = "id") @NotNull Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return EntityModel.of(category,
                linkTo(methodOn(CategoryController.class).getCategoryById(id)).withSelfRel(),
                linkTo(methodOn(CategoryController.class).getCategories(Pageable.unpaged())).withRel("categories"),
                linkTo(methodOn(CategoryController.class).updateCategory(id, null)).withRel("update"),
                linkTo(methodOn(CategoryController.class).deleteCategory(id)).withRel("delete"));
    }

    @Operation(summary = "Buscar categorias por tipo", description = "Consulta personalizada paginada por enum CategoryType. Valores aceitos: RPG, ACTION, ADVENTURE, SPORTS e STRATEGY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "Tipo inválido. Use: RPG, ACTION, ADVENTURE, SPORTS ou STRATEGY", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Tipo inválido. Use: RPG, ACTION, ADVENTURE, SPORTS ou STRATEGY\"")))
    })
    @GetMapping("/categories/search")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PagedModel<EntityModel<Category>>> searchCategoriesByType(
            @Parameter(description = "Tipo da categoria", example = "RPG", required = true)
            @RequestParam CategoryType type,
            @ParameterObject Pageable pageable) {
        var page = categoryRepository.findByType(type, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                category -> EntityModel.of(category,
                        linkTo(methodOn(CategoryController.class).getCategoryById(category.getId())).withSelfRel(),
                        linkTo(methodOn(CategoryController.class).updateCategory(category.getId(), null)).withRel("update"),
                        linkTo(methodOn(CategoryController.class).deleteCategory(category.getId())).withRel("delete"))));
    }

    @Operation(summary = "Criar categoria", description = "Cria uma categoria do catálogo. Depois de criada, ela pode ser vinculada a vários jogos. Requer X-API-Key e X-Idempotency-Key.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "X-Idempotency-Key ausente ou em branco", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key header é obrigatório\""))),
            @ApiResponse(responseCode = "409", description = "Mesma X-Idempotency-Key usada com payload diferente", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"X-Idempotency-Key já utilizada com payload diferente\"")))
    })
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Category> createCategory(
            @Parameter(description = "Chave única para garantir idempotência", required = true)
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da categoria a ser criada", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"type\": \"RPG\" }")))
            @Valid @RequestBody Category newCategory) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var requestFingerprint = new CreateCategoryFingerprint(
                newCategory.getType() != null ? newCategory.getType().name() : null
        );

        synchronized (createCategoryIdempotencyLock) {
            var existing = createCategoryResponses.get(idempotencyKey);

            if (existing != null) {
                if (existing.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.created(existing.location()).body(existing.category());
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }

            var saved = categoryRepository.save(newCategory);
            var location = URI.create("/categories/" + saved.getId());
            createCategoryResponses.put(idempotencyKey, new IdempotentCreateResponse(requestFingerprint, saved, location));
            return ResponseEntity.created(location).body(saved);
        }
    }

    @Operation(summary = "Atualizar categoria", description = "Atualiza o tipo da categoria. Jogos vinculados continuam usando a mesma categoria pelo relacionamento One-to-Many.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoria atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Dados inválidos\""))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find category 1\"")))
    })
    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "ID da categoria", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados atualizados da categoria", required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"type\": \"ACTION\" }")))
            @Valid @RequestBody Category updatedCategory) {
        return categoryRepository.findById(id).map(category -> {
            category.setType(updatedCategory.getType());
            return ResponseEntity.ok(categoryRepository.save(category));
        }).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Operation(summary = "Deletar categoria", description = "Remove uma categoria pelo ID. Se houver jogos vinculados por cascade, eles também podem ser afetados conforme o mapeamento JPA.", security = @SecurityRequirement(name = "ApiKeyAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Categoria deletada com sucesso"),
            @ApiResponse(responseCode = "400", description = "ID inválido informado", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"ID inválido informado\""))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Could not find category 1\"")))
    })
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID da categoria", example = "1", required = true)
            @PathVariable Long id) {
        var category = categoryRepository.findById(id).orElse(null);
        if (category == null) return ResponseEntity.notFound().build();
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
