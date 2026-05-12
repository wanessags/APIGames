package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Schema(description = "Representa um usuário")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do usuário", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 1, max = 255)
    @Schema(description = "Nome do usuário", example = "Wanessa")
    private String name;

    @NotBlank
    @Email
    @Size(min = 1, max = 255)
    @Column(unique = true)
    @Schema(description = "Email do usuário", example = "wanessa@email.com")
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference("user-review")
    @Schema(description = "Reviews feitas pelo usuário")
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference("user-api-key")
    @Schema(description = "Chaves de API pertencentes ao usuário")
    private List<ApiKey> apiKeys = new ArrayList<>();

    public User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public User(Long id, String name, String email, List<Review> reviews) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.reviews = reviews;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
