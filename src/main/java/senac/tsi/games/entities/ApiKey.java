package senac.tsi.games.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Schema(description = "Chave de API associada a um usuário para autenticar endpoints sensíveis")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador da chave", example = "1")
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Rótulo da chave para identificação", example = "Postman")
    private String label;

    @NotBlank
    @Column(nullable = false, unique = true, length = 120)
    @Schema(description = "Valor que deve ser enviado no header X-API-Key")
    private String keyValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Nível de acesso da chave", example = "WRITE")
    private ApiKeyRole role = ApiKeyRole.WRITE;

    @Column(nullable = false)
    @Schema(description = "Indica se a chave ainda está ativa", example = "true")
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Data de criação da chave")
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-api-key")
    @Schema(description = "Usuário dono da chave")
    private User user;

    public ApiKey() {
    }

    public ApiKey(String label, String keyValue, ApiKeyRole role, User user) {
        this.label = label;
        this.keyValue = keyValue;
        this.role = role == null ? ApiKeyRole.WRITE : role;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public ApiKeyRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public void setRole(ApiKeyRole role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
