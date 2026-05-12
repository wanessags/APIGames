package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Schema(description = "Representa os detalhes de um jogo")
public class GameDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do detalhe", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 1, max = 255)
    @Schema(description = "Descrição do jogo", example = "Jogo de aventura incrível")
    private String description;

    @NotBlank
    @Size(min = 1, max = 255)
    @Schema(description = "Desenvolvedora do jogo", example = "Ubisoft")
    private String developer;

    @OneToOne
    @JoinColumn(name = "game_id")
    @NotNull
    @JsonBackReference("game-detail")
    @Schema(description = "Jogo relacionado a este detalhe")
    private Game game;

    public GameDetail() {}

    public GameDetail(String description, String developer) {
        this.description = description;
        this.developer = developer;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDeveloper() {
        return developer;
    }

    public Game getGame() {
        return game;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public String toString() {
        return "GameDetail{id=" + id + ", description='" + description + "', developer='" + developer + "'}";
    }
}
