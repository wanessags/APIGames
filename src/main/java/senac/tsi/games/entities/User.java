package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

// @Entity avisa ao JPA que esta classe deve ser tratada como entidade persistente no banco de dados.
@Entity
// @Table é usado aqui para definir explicitamente o nome da tabela e evitar conflitos com nomes reservados do banco.
@Table(name = "users")
// A classe User representa um registro da aplicação e será mapeada para uma tabela no banco.
public class User {

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
// @NotBlank é importante para Strings, porque além de impedir null também impede texto vazio ou só com espaços.
    @NotBlank
// @Email valida o formato do e-mail antes de tentar persistir os dados.
    @Email
    private String email;

    // @OneToOne representa um vínculo exclusivo entre dois registros: um de cada lado.
    @OneToOne(mappedBy = "user")
// @JsonIgnoreProperties evita loops infinitos no JSON quando as entidades se referenciam em ambos os lados do relacionamento.
    @JsonIgnoreProperties({"game", "user"})
    private Review review;

    public User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getter usado para expor o valor de id de forma controlada.
    public Long getId() {
        return id;
    }

    // Getter usado para expor o valor de name de forma controlada.
    public String getName() {
        return name;
    }

    // Getter usado para expor o valor de email de forma controlada.
    public String getEmail() {
        return email;
    }

    // Getter usado para expor o valor de review de forma controlada.
    public Review getReview() {
        return review;
    }

    // Setter usado para atualizar o valor de id quando a aplicação precisa alterar o objeto.
    public void setId(Long id) {
        this.id = id;
    }

    // Setter usado para atualizar o valor de name quando a aplicação precisa alterar o objeto.
    public void setName(String name) {
        this.name = name;
    }

    // Setter usado para atualizar o valor de email quando a aplicação precisa alterar o objeto.
    public void setEmail(String email) {
        this.email = email;
    }

    // Setter usado para atualizar o valor de review quando a aplicação precisa alterar o objeto.
    public void setReview(Review review) {
        this.review = review;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(name, user.name) &&
                Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}