package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

// @Entity avisa ao JPA que esta classe deve ser tratada como entidade persistente no banco de dados.
@Entity
// A classe Review representa um registro da aplicação e será mapeada para uma tabela no banco.
public class Review {

    // @Id marca o atributo que funciona como chave primária, ou seja, o identificador único de cada registro.
    @Id
// @GeneratedValue informa que o valor do identificador será gerado automaticamente, sem precisar ser enviado no POST.
    @GeneratedValue
    private Long id;

    // @NotNull garante que esse campo não seja salvo como nulo, reforçando a validação da regra de negócio.
    @NotNull
// @NotBlank é importante para Strings, porque além de impedir null também impede texto vazio ou só com espaços.
    @NotBlank
// @Size limita o tamanho do texto para evitar valores curtos demais ou grandes demais no banco.
    @Size(min = 1, max = 255)
    private String comment;

    // @NotNull garante que esse campo não seja salvo como nulo, reforçando a validação da regra de negócio.
    @NotNull
// @Min define o valor mínimo aceito para o campo numérico.
    @Min(0)
// @Max define o valor máximo aceito para o campo numérico.
    @Max(10)
    private Integer score;

    // @ManyToOne representa o lado 'muitos para um', ou seja, vários registros podem apontar para o mesmo objeto relacionado.
    @ManyToOne
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties({"reviews", "category", "platforms"})
    private Game game;

    // @OneToOne representa um vínculo exclusivo entre dois registros: um de cada lado.
    @OneToOne
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties("review")
    private User user;

    public Review() {
    }

    public Review(String comment, Integer score) {
        this.comment = comment;
        this.score = score;
    }

    public Review(Long id, String comment, Integer score) {
        this.id = id;
        this.comment = comment;
        this.score = score;
    }

    // Getter usado para expor o valor de id de forma controlada.
    public Long getId() {
        return id;
    }

    // Getter usado para expor o valor de comment de forma controlada.
    public String getComment() {
        return comment;
    }

    // Getter usado para expor o valor de score de forma controlada.
    public Integer getScore() {
        return score;
    }

    // Getter usado para expor o valor de game de forma controlada.
    public Game getGame() {
        return game;
    }

    // Getter usado para expor o valor de user de forma controlada.
    public User getUser() {
        return user;
    }

    // Setter usado para atualizar o valor de id quando a aplicação precisa alterar o objeto.
    public void setId(Long id) {
        this.id = id;
    }

    // Setter usado para atualizar o valor de comment quando a aplicação precisa alterar o objeto.
    public void setComment(String comment) {
        this.comment = comment;
    }

    // Setter usado para atualizar o valor de score quando a aplicação precisa alterar o objeto.
    public void setScore(Integer score) {
        this.score = score;
    }

    // Setter usado para atualizar o valor de game quando a aplicação precisa alterar o objeto.
    public void setGame(Game game) {
        this.game = game;
    }

    // Setter usado para atualizar o valor de user quando a aplicação precisa alterar o objeto.
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return Objects.equals(id, review.id) &&
                Objects.equals(comment, review.comment) &&
                Objects.equals(score, review.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, comment, score);
    }

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", comment='" + comment + '\'' +
                ", score=" + score +
                '}';
    }
}