package senac.tsi.games.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import senac.tsi.games.entities.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByGameId(Long gameId, Pageable pageable);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.game.id = :gameId")
    Double getAverageScoreByGameId(Long gameId);
}
