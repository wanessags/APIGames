package senac.tsi.games.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import senac.tsi.games.entities.ApiKey;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);

    Page<ApiKey> findByUserId(Long userId, Pageable pageable);

    Page<ApiKey> findByLabelContainingIgnoreCase(String label, Pageable pageable);
}
