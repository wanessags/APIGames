/*
 * Este arquivo é um controller da API.
 * No padrão do Spring Boot, o controller é a camada responsável por receber as requisições HTTP
 * (GET, POST, PUT e DELETE), chamar o repository correspondente e devolver a resposta ao cliente.
 * Aqui também estão as anotações de documentação Swagger e os recursos de HATEOAS/paginação.
 */
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
import senac.tsi.games.entities.Category;
import senac.tsi.games.entities.CategoryType;
import senac.tsi.games.exceptions.CategoryNotFoundException;
import senac.tsi.games.repositories.CategoryRepository;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// @Tag agrupa e organiza os endpoints no Swagger, facilitando a leitura da documentação.
@Tag(name = "categories", description = "Categories route")
@RestController

// A classe CategoryController expõe os endpoints HTTP dessa entidade e organiza o comportamento REST da API.
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final PagedResourcesAssembler<Category> pagedAssembler;

    // @Autowired aqui reforça que o Spring deve fornecer automaticamente as dependências no construtor.
    @Autowired
    public CategoryController(CategoryRepository categoryRepository,
                              PagedResourcesAssembler<Category> pagedAssembler) {
        this.categoryRepository = categoryRepository;
        this.pagedAssembler = pagedAssembler;
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get all categories", description = "Get all categories with pagination and HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Categories found successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/categories")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
// Este método atende uma operação de leitura e devolve dados de CategoryController pela API.
// Aqui a resposta usa paginação e HATEOAS ao mesmo tempo: por isso o retorno é mais rico do que uma simples List.
    public ResponseEntity<PagedModel<EntityModel<Category>>> getCategories(@ParameterObject Pageable pageable) {

        var page = categoryRepository.findAll(pageable);

// O pagedAssembler transforma a página retornada pelo JPA em um modelo HATEOAS paginado, com links embutidos.
        PagedModel<EntityModel<Category>> model = pagedAssembler.toModel(
                page,
// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
                category -> EntityModel.of(category,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(CategoryController.class).getCategoryById(category.getId())).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(CategoryController.class).getCategories(pageable)).withRel("categories"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(CategoryController.class).updateCategory(category.getId(), null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(CategoryController.class).deleteCategory(category.getId())).withRel("delete"))
        );

// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(model);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get category by id", description = "Get one category by id with HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Category.class)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid id supplied"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/categories/{id}")
// Este método atende uma operação de leitura e devolve dados de CategoryController pela API.
    public EntityModel<Category> getCategoryById(
            @PathVariable(name = "id", required = true)
            @NotNull Long id) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        Category category = categoryRepository.findById(id)
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new CategoryNotFoundException(id));

// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
        return EntityModel.of(category,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(CategoryController.class).getCategoryById(id)).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(CategoryController.class).getCategories(Pageable.unpaged())).withRel("categories"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(CategoryController.class).updateCategory(id, null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(CategoryController.class).deleteCategory(id)).withRel("delete"));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Search categories by type", description = "Search categories filtering by enum type")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Search executed successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/categories/search")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Category>> searchCategoriesByType(@RequestParam CategoryType type) {
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(categoryRepository.findByType(type));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Create category", description = "Create a new category")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "Category created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Category.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "RPG"
                                    }
                                    """)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PostMapping marca um endpoint de criação, usado quando queremos inserir um novo recurso.
    @PostMapping("/categories")
// O status 201 indica que um novo recurso foi criado com sucesso.
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Category> createCategory(@RequestBody Category newCategory) {
        Category savedCategory = categoryRepository.save(newCategory);

// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
        return ResponseEntity.created(
                        URI.create("/categories/" + savedCategory.getId()))
                .body(savedCategory);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Update category", description = "Update an existing category")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PutMapping marca um endpoint de atualização, normalmente usado para alterar um recurso já existente.
    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id,
                                                   @RequestBody Category updatedCategory) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        return categoryRepository.findById(id).map(
                category -> {
                    category.setType(updatedCategory.getType());
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
                    return ResponseEntity.ok(categoryRepository.save(category));
                }
        ).orElseGet(() -> {
            Category savedCategory = categoryRepository.save(updatedCategory);
// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
            return ResponseEntity.created(
                            URI.create("/categories/" + savedCategory.getId()))
                    .body(savedCategory);
        });
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Delete category", description = "Delete a category by id")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
// @DeleteMapping marca um endpoint de remoção de recurso.
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        var category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
// notFound devolve 404 sem corpo quando o recurso pedido para remoção não existe.
            return ResponseEntity.notFound().build();
        }

        categoryRepository.deleteById(id);
// noContent devolve 204, que é o status correto quando a exclusão funciona e não há corpo de resposta.
        return ResponseEntity.noContent().build();
    }
}