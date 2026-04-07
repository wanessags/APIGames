package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

// @Entity avisa ao JPA que esta classe deve ser tratada como entidade persistente no banco de dados.
@Entity
// A classe Platform representa um registro da aplicação e será mapeada para uma tabela no banco.
public class Platform {

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

    // @ManyToMany é usado quando os dois lados podem ter múltiplos vínculos entre si.
    @ManyToMany(mappedBy = "platforms")
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties({"platforms", "category", "reviews"})
    private List<Game> games;

    public Platform() {
    }

    public Platform(String name) {
        this.name = name;
    }

    public Platform(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getter usado para expor o valor de id de forma controlada.
    public Long getId() {
        return id;
    }

    // Getter usado para expor o valor de name de forma controlada.
    public String getName() {
        return name;
    }

    // Getter usado para expor o valor de games de forma controlada.
    public List<Game> getGames() {
        return games;
    }

    // Setter usado para atualizar o valor de id quando a aplicação precisa alterar o objeto.
    public void setId(Long id) {
        this.id = id;
    }

    // Setter usado para atualizar o valor de name quando a aplicação precisa alterar o objeto.
    public void setName(String name) {
        this.name = name;
    }

    // Setter usado para atualizar o valor de games quando a aplicação precisa alterar o objeto.
    public void setGames(List<Game> games) {
        this.games = games;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Platform platform = (Platform) o;
        return Objects.equals(id, platform.id) && Objects.equals(name, platform.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Platform{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}