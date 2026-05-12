package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Schema(description = "Representa uma plataforma")
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID da plataforma", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 1, max = 255)
    @Schema(description = "Nome da plataforma", example = "PlayStation 5")
    private String name;

    // 🔥 AQUI ESTÁ O QUE FALTAVA
    @ManyToMany(mappedBy = "platforms")
    @JsonBackReference("game-platform")
    @Schema(description = "Jogos disponíveis nessa plataforma")
    private List<Game> games = new ArrayList<>();

    public Platform() {
    }

    public Platform(String name) {
        this.name = name;
    }

    public Platform(Long id, String name, List<Game> games) {
        this.id = id;
        this.name = name;
        this.games = games;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Game> getGames() {
        return games;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }
}