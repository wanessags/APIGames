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
import senac.tsi.games.entities.Platform;
import senac.tsi.games.exceptions.PlatformNotFoundException;
import senac.tsi.games.repositories.PlatformRepository;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// @Tag agrupa e organiza os endpoints no Swagger, facilitando a leitura da documentação.
@Tag(name = "platforms", description = "Platforms route")
@RestController
// A classe PlatformController expõe os endpoints HTTP dessa entidade e organiza o comportamento REST da API.
public class PlatformController {

    private final PlatformRepository platformRepository;
    private final PagedResourcesAssembler<Platform> pagedAssembler;

    // @Autowired aqui reforça que o Spring deve fornecer automaticamente as dependências no construtor.
    @Autowired
    public PlatformController(PlatformRepository platformRepository,
                              PagedResourcesAssembler<Platform> pagedAssembler) {
        this.platformRepository = platformRepository;
        this.pagedAssembler = pagedAssembler;
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get all platforms", description = "Get all platforms with pagination and HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Platforms found successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/platforms")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
// Este método atende uma operação de leitura e devolve dados de PlatformController pela API.
// Aqui a resposta usa paginação e HATEOAS ao mesmo tempo: por isso o retorno é mais rico do que uma simples List.
    public ResponseEntity<PagedModel<EntityModel<Platform>>> getPlatforms(@ParameterObject Pageable pageable) {

        var page = platformRepository.findAll(pageable);

// O pagedAssembler transforma a página retornada pelo JPA em um modelo HATEOAS paginado, com links embutidos.
        PagedModel<EntityModel<Platform>> model = pagedAssembler.toModel(
                page,
// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
                platform -> EntityModel.of(platform,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(PlatformController.class).getPlatformById(platform.getId())).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(PlatformController.class).getPlatforms(pageable)).withRel("platforms"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(PlatformController.class).updatePlatform(platform.getId(), null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                        linkTo(methodOn(PlatformController.class).deletePlatform(platform.getId())).withRel("delete"))
        );

// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(model);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Get platform by id", description = "Get one platform by id with HATEOAS links")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Platform found",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Platform.class)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid id supplied"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "Platform not found")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/platforms/{id}")
// Este método atende uma operação de leitura e devolve dados de PlatformController pela API.
    public EntityModel<Platform> getPlatformById(
            @PathVariable(name = "id", required = true)
            @NotNull Long id) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        Platform platform = platformRepository.findById(id)
// orElseThrow evita retornar null silenciosamente e garante a resposta 404 quando o recurso não existe.
                .orElseThrow(() -> new PlatformNotFoundException(id));

// EntityModel.of envolve o objeto principal e adiciona links de navegação, como self, update e delete.
        return EntityModel.of(platform,
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(PlatformController.class).getPlatformById(id)).withSelfRel(),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(PlatformController.class).getPlatforms(Pageable.unpaged())).withRel("platforms"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(PlatformController.class).updatePlatform(id, null)).withRel("update"),
// linkTo(methodOn(...)) cria links baseados nos próprios métodos do controller, evitando URL escrita manualmente.
                linkTo(methodOn(PlatformController.class).deletePlatform(id)).withRel("delete"));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Search platforms by name", description = "Search platforms filtering by name")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Search executed successfully")
    })
// @GetMapping marca um endpoint de leitura, usado para listar recursos ou buscar um item específico.
    @GetMapping("/platforms/search")
// O status 200 indica que a requisição foi processada com sucesso.
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Platform>> searchPlatformsByName(@RequestParam String name) {
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
        return ResponseEntity.ok(platformRepository.findByNameContainingIgnoreCase(name));
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Create platform", description = "Create a new platform")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "Platform created successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Platform.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "PC"
                                    }
                                    """)) }),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PostMapping marca um endpoint de criação, usado quando queremos inserir um novo recurso.
    @PostMapping("/platforms")
// O status 201 indica que um novo recurso foi criado com sucesso.
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Platform> createPlatform(@RequestBody Platform newPlatform) {
        Platform savedPlatform = platformRepository.save(newPlatform);

// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
        return ResponseEntity.created(
                        URI.create("/platforms/" + savedPlatform.getId()))
                .body(savedPlatform);
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Update platform", description = "Update an existing platform")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "200", description = "Platform updated successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "201", description = "Platform created successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
// @PutMapping marca um endpoint de atualização, normalmente usado para alterar um recurso já existente.
    @PutMapping("/platforms/{id}")
    public ResponseEntity<Platform> updatePlatform(@PathVariable Long id,
                                                   @RequestBody Platform updatedPlatform) {

// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        return platformRepository.findById(id).map(
                platform -> {
                    platform.setName(updatedPlatform.getName());
// ResponseEntity.ok devolve 200 quando a operação termina normalmente e o conteúdo pode ser enviado no corpo.
                    return ResponseEntity.ok(platformRepository.save(platform));
                }
        ).orElseGet(() -> {
            Platform savedPlatform = platformRepository.save(updatedPlatform);
// ResponseEntity.created devolve o status 201 e ainda informa a URI do recurso recém-criado.
            return ResponseEntity.created(
                            URI.create("/platforms/" + savedPlatform.getId()))
                    .body(savedPlatform);
        });
    }

    // @Operation documenta o objetivo do endpoint para que o Swagger mostre a intenção de uso de forma clara.
    @Operation(summary = "Delete platform", description = "Delete a platform by id")
// @ApiResponses descreve os códigos de resposta possíveis, o que ajuda muito tanto na apresentação quanto no consumo da API.
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
    @ApiResponses(value = {
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "204", description = "Platform deleted successfully"),
// Cada @ApiResponse explica um status HTTP possível para a operação documentada logo abaixo.
            @ApiResponse(responseCode = "404", description = "Platform not found")
    })
// @DeleteMapping marca um endpoint de remoção de recurso.
    @DeleteMapping("/platforms/{id}")
    public ResponseEntity<Void> deletePlatform(@PathVariable Long id) {
// Primeiro a API tenta localizar o recurso pelo id; se não encontrar, lança a exceção personalizada da entidade.
        var platform = platformRepository.findById(id).orElse(null);

        if (platform == null) {
// notFound devolve 404 sem corpo quando o recurso pedido para remoção não existe.
            return ResponseEntity.notFound().build();
        }

        platformRepository.deleteById(id);
// noContent devolve 204, que é o status correto quando a exclusão funciona e não há corpo de resposta.
        return ResponseEntity.noContent().build();
    }
}