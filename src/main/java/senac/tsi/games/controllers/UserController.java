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
import senac.tsi.games.entities.User;
import senac.tsi.games.exceptions.UserNotFoundException;
import senac.tsi.games.repositories.UserRepository;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// @Tag agrupa e organiza os endpoints no Swagger, facilitando a leitura da documentação.
@Tag(name = "users", description = "Users route")
@RestController
// A classe UserController expõe os endpoints HTTP dessa entidade e organiza o comportamento REST da API.
public class UserController {

    private final UserRepository userRepository;
    private final PagedResourcesAssembler<User> pagedAssembler;

    // @Autowired aqui reforça que o Spring deve fornecer automaticamente as dependências no construtor.
    @Autowired
    public UserController(UserRepository userRepository,
                          PagedResourcesAssembler<User> pagedAssembler) {
        this.userRepository = userRepository;
        this.pagedAssembler = pagedAssembler;
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get all users", description = "Get all users with pagination and HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Users found successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/users")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
// Este método atende uma operação de leitura e devolve dados de UserController pela API.
// Aqui a resposta usa paginação e HATEOAS ao mesmo tempo: por isso o retorno é mais rico do que uma simples List.
    public ResponseEntity<PagedModel<EntityModel<User>>> getUsers(@ParameterObject Pageable pageable) {

        var page = userRepository.findAll(pageable);

// O pagedAssembler transforma a página retornada pelo JPA em um modelo HATEOAS paginado, com links embutidos.
        PagedModel<EntityModel<User>> model = pagedAssembler.toModel(
                page,
// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
                user -> EntityModel.of(user,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(UserController.class).getUserById(user.getId())).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(UserController.class).getUsers(pageable)).withRel("users"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(UserController.class).updateUser(user.getId(), null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(UserController.class).deleteUser(user.getId())).withRel("delete"))
        );

// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(model);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get user by id", description = "Get one user by id with HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "User found",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid id supplied"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "User not found")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/users/{id}")
// Este método atende uma operação de leitura e devolve dados de UserController pela API.
    public EntityModel<User> getUserById(
            @PathVariable(name = "id", required = true)
            @NotNull Long id) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        User user = userRepository.findById(id)
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new UserNotFoundException(id));

// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
        return EntityModel.of(user,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(UserController.class).getUsers(Pageable.unpaged())).withRel("users"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Search users by email", description = "Search users filtering by email")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Search executed successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/users/search")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<User>> searchUsersByEmail(@RequestParam String email) {
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(userRepository.findByEmailContainingIgnoreCase(email));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Create user", description = "Create a new user")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "Wanessa",
                                      "email": "wanessa@email.com"
                                    }
                                    """)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PostMapping marca um endpoint de criação, usado quando queremos inserir um novo recurso.
    @PostMapping("/users")
// O status 201 indica que um novo recurso foi criado com sucesso.
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<User> createUser(@RequestBody User newUser) {
        User savedUser = userRepository.save(newUser);

// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
        return ResponseEntity.created(
                        URI.create("/users/" + savedUser.getId()))
                .body(savedUser);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Update user", description = "Update an existing user")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "User created successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PutMapping marca um endpoint de atualização, normalmente usado para alterar um recurso já existente.
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @RequestBody User updatedUser) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        return userRepository.findById(id).map(
                user -> {
                    user.setName(updatedUser.getName());
                    user.setEmail(updatedUser.getEmail());
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
                    return ResponseEntity.ok(userRepository.save(user));
                }
        ).orElseGet(() -> {
            User savedUser = userRepository.save(updatedUser);
// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
            return ResponseEntity.created(
                            URI.create("/users/" + savedUser.getId()))
                    .body(savedUser);
        });
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Delete user", description = "Delete a user by id")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "User not found")
    })
// @DeleteMapping marca um endpoint de remoção de recurso.
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        var user = userRepository.findById(id).orElse(null);

        if (user == null) {
// notFound devolve 404 sem corpo quando o recurso pedido para remoção não existe.
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);
// noContent devolve 204, que é o status correto quando a exclusão funciona e não há corpo de resposta.
        return ResponseEntity.noContent().build();
    }
}