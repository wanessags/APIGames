/*
 * Este arquivo representa uma entidade da aplicação.
 * Em JPA, uma entidade vira uma tabela no banco de dados.
 * Por isso ela recebe anotações como @Entity, @Id, validações e relacionamentos.
 * Como você pediu, os comentários abaixo só explicam o código; a lógica original foi mantida.
 */
package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

// @Entity avisa ao JPA que esta classe deve ser tratada como entidade persistente no banco de dados.
@Entity
// A classe Category representa um registro da aplicação e será mapeada para uma tabela no banco.
public class Category {

    // @Id marca o atributo que funciona como chave primária, ou seja, o identificador único de cada registro.
    @Id
// @GeneratedValue informa que o valor do identificador será gerado automaticamente, sem precisar ser enviado no POST.
    @GeneratedValue
    private Long id;

    // @NotNull garante que esse campo não seja salvo como nulo, reforçando a validação da regra de negócio.
    @NotNull
// @Enumerated(EnumType.STRING) faz o enum ser salvo como texto, o que deixa o banco mais legível e seguro.
    @Enumerated(EnumType.STRING)
    private CategoryType type;

    // @OneToMany representa um relacionamento em que um registro desta entidade pode estar ligado a vários da outra.
    @OneToMany(mappedBy = "category")
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties({"category", "platforms", "reviews"})
    private List<Game> games;

    public Category() {
    }

    public Category(CategoryType type) {
        this.type = type;
    }

    public Category(Long id, CategoryType type) {
        this.id = id;
        this.type = type;
    }

    // Getter usado para expor o valor de id de forma controlada.
    public Long getId() {
        return id;
    }

    // Getter usado para expor o valor de type de forma controlada.
    public CategoryType getType() {
        return type;
    }

    // Getter usado para expor o valor de games de forma controlada.
    public List<Game> getGames() {
        return games;
    }

    // Setter usado para atualizar o valor de id quando a aplicação precisa alterar o objeto.
    public void setId(Long id) {
        this.id = id;
    }

    // Setter usado para atualizar o valor de type quando a aplicação precisa alterar o objeto.
    public void setType(CategoryType type) {
        this.type = type;
    }

    // Setter usado para atualizar o valor de games quando a aplicação precisa alterar o objeto.
    public void setGames(List<Game> games) {
        this.games = games;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) && type == category.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}