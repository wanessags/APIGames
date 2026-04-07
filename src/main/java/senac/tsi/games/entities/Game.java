package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

// @Entity avisa ao JPA que esta classe deve ser tratada como entidade persistente no banco de dados.
@Entity
// A classe Game representa um registro da aplicação e será mapeada para uma tabela no banco.
public class Game {

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
    private String name;

    // @NotNull garante que esse campo não seja salvo como nulo, reforçando a validação da regra de negócio.
    @NotNull
    private Double price;

    // @ManyToOne representa o lado 'muitos para um', ou seja, vários registros podem apontar para o mesmo objeto relacionado.
    @ManyToOne
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties("games")
    private Category category;

    // @ManyToMany é usado quando os dois lados podem ter múltiplos vínculos entre si.
    @ManyToMany
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties("games")
    private List<Platform> platforms;

    // @OneToMany representa um relacionamento em que um registro desta entidade pode estar ligado a vários da outra.
    @OneToMany(mappedBy = "game")
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties({"game", "user"})
    private List<Review> reviews;

    public Game() {
    }

    public Game(String name, Double price) {
        this.name = name;
        this.price = price;
    }

    public Game(Long id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getter usado para expor o valor de id de forma controlada.
    public Long getId() {
        return id;
    }

    // Getter usado para expor o valor de name de forma controlada.
    public String getName() {
        return name;
    }

    // Getter usado para expor o valor de price de forma controlada.
    public Double getPrice() {
        return price;
    }

    // Getter usado para expor o valor de category de forma controlada.
    public Category getCategory() {
        return category;
    }

    // Getter usado para expor o valor de platforms de forma controlada.
    public List<Platform> getPlatforms() {
        return platforms;
    }

    // Getter usado para expor o valor de reviews de forma controlada.
    public List<Review> getReviews() {
        return reviews;
    }

    // Setter usado para atualizar o valor de id quando a aplicação precisa alterar o objeto.
    public void setId(Long id) {
        this.id = id;
    }

    // Setter usado para atualizar o valor de name quando a aplicação precisa alterar o objeto.
    public void setName(String name) {
        this.name = name;
    }

    // Setter usado para atualizar o valor de price quando a aplicação precisa alterar o objeto.
    public void setPrice(Double price) {
        this.price = price;
    }

    // Setter usado para atualizar o valor de category quando a aplicação precisa alterar o objeto.
    public void setCategory(Category category) {
        this.category = category;
    }

    // Setter usado para atualizar o valor de platforms quando a aplicação precisa alterar o objeto.
    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    // Setter usado para atualizar o valor de reviews quando a aplicação precisa alterar o objeto.
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(id, game.id) &&
                Objects.equals(name, game.name) &&
                Objects.equals(price, game.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price);
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}