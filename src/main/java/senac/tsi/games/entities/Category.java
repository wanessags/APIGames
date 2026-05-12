package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Schema(description = "Representa a categoria de um jogo")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador da categoria", example = "1")
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Schema(description = "Tipo da categoria", example = "RPG")
    private CategoryType type;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonManagedReference("category-game")
    @Schema(description = "Jogos pertencentes à categoria")
    private List<Game> games = new ArrayList<>();

    public Category() {
    }

    public Category(CategoryType type) {
        this.type = type;
    }

    public Category(Long id, CategoryType type, List<Game> games) {
        this.id = id;
        this.type = type;
        this.games = games;
    }

    public Long getId() {
        return id;
    }

    public CategoryType getType() {
        return type;
    }

    public List<Game> getGames() {
        return games;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", type=" + type + "}";
    }
}
