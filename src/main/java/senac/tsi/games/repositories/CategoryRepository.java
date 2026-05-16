package senac.tsi.games.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.tsi.games.entities.Category;
import senac.tsi.games.entities.CategoryType;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findByType(CategoryType type, Pageable pageable);

    boolean existsByType(CategoryType type);
}
