package senac.tsi.games.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.games.entities.GameDetail;

import java.util.Optional;

@Repository
public interface GameDetailRepository extends JpaRepository<GameDetail, Long> {

    // Busca o detalhe pelo ID do jogo vinculado
    Optional<GameDetail> findByGameId(Long gameId);
}