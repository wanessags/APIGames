/*
 * Este arquivo é um repository.
 * No Spring Data JPA, o repository é a interface que faz a ponte entre o código Java e o banco.
 * Ao estender JpaRepository, ele já ganha operações prontas de CRUD sem precisar escrever SQL manualmente.
 * Além disso, aqui também existem consultas personalizadas derivadas do nome do método.
 */
package senac.tsi.games.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.games.entities.Platform;

import java.util.List;

// @Repository marca a interface como componente de acesso a dados, permitindo que o Spring a injete automaticamente.
@Repository
// A interface PlatformRepository herda recursos do Spring Data JPA e evita que a gente escreva o CRUD manualmente.
public interface PlatformRepository extends JpaRepository<Platform, Long> {

    // Este método é uma consulta derivada do Spring Data JPA: o próprio nome do método já diz ao framework como filtrar os dados.
    List<Platform> findByNameContainingIgnoreCase(String name);
}