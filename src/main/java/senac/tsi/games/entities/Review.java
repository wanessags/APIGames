package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Schema(description = "Representa uma avaliação de um jogo")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador da review", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 1, max = 255)
    @Schema(description = "Comentário da review", example = "Excelente")
    private String comment;

    @Min(0)
    @Max(10)
    @NotNull
    @Schema(description = "Nota da review", example = "10")
    private Integer score;

    @ManyToOne
    @JoinColumn(name = "game_id")
    @NotNull
    @JsonBackReference("game-review")
    @Schema(description = "Jogo relacionado à review")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @NotNull
    @JsonBackReference("user-review")
    @Schema(description = "Usuário que fez a review")
    private User user;

    public Review() {
    }

    public Review(String comment, Integer score) {
        this.comment = comment;
        this.score = score;
    }

    public Review(Long id, String comment, Integer score, Game game, User user) {
        this.id = id;
        this.comment = comment;
        this.score = score;
        this.game = game;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public Integer getScore() {
        return score;
    }

    public Game getGame() {
        return game;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Review{id=" + id + ", comment='" + comment + "', score=" + score + "}";
    }
}
