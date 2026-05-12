package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Schema(description = "Representa um jogo do catálogo. É o recurso central da API e se relaciona com categoria, plataformas, reviews e detalhe.")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do jogo", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(min = 1, max = 255)
    @Schema(description = "Nome do jogo", example = "The Witcher 3")
    private String name;

    @NotNull
    @Positive
    @Schema(description = "Preco do jogo", example = "99.90")
    private Double price;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @NotNull
    @JsonBackReference("category-game")
    @Schema(description = "Categoria do jogo. Relação Many-to-One: vários jogos podem pertencer a mesma categoria.")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "game_platforms",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    @JsonManagedReference("game-platform")
    @Schema(description = "Plataformas em que o jogo está disponível. Relação Many-to-Many com Platform.")
    private List<Platform> platforms = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    @JsonManagedReference("game-review")
    @Schema(description = "Reviews recebidas pelo jogo. Relação One-to-Many com Review.", accessMode = Schema.AccessMode.READ_ONLY)
    private List<Review> reviews = new ArrayList<>();

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL)
    @JsonManagedReference("game-detail")
    @Schema(description = "Detalhe complementar do jogo. Relação One-to-One com GameDetail.", accessMode = Schema.AccessMode.READ_ONLY)
    private GameDetail detail;

    public Game() {}

    public Game(String name, Double price) {
        this.name = name;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public GameDetail getDetail() {
        return detail;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void setDetail(GameDetail detail) {
        this.detail = detail;
    }
}
