package senac.tsi.games.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.games.entities.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Page<Game> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
